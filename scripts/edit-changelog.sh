#!/usr/bin/env bash
#
# Edit a release's changelog once, everywhere.
#
# Pulls the notes from the GitHub release, opens them in your editor, and on save pushes the
# result to the GitHub release, every Modrinth version and every CurseForge file belonging to
# that same tag. Twelve targets means twelve platform versions per release, which is far too
# many to correct by hand.
#
#   scripts/edit-changelog.sh                 # latest release
#   scripts/edit-changelog.sh 1.0.0-b1        # a specific tag
#   scripts/edit-changelog.sh --dry-run       # show what would change, write nothing
#
# Credentials are read from the environment or from .env in the repository root, using the same
# names the release workflow uses:
#
#   PUB_MODRINTH_PROJECT_ID    PUB_MODRINTH_TOKEN
#   PUB_CURSEFORGE_PROJECT_ID  PUB_CURSEFORGE_TOKEN
#
# A platform whose credentials are missing is skipped with a notice rather than failing the run,
# so this is useful with only one of them configured.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MODRINTH_API="https://api.modrinth.com/v2"
CURSEFORGE_UPLOAD_API="https://minecraft.curseforge.com/api"
# Listing a project's files is not part of CurseForge's upload API, and their official read API
# needs a separately approved key. This is the endpoint the CurseForge website itself calls; it
# needs no key. Unofficial, so it is treated as best-effort — if it stops working, the script
# says so and prints the file IDs to update by hand rather than guessing.
CURSEFORGE_LIST_API="https://www.curseforge.com/api/v1"

TAG=""
DRY_RUN=false
ASSUME_YES=false
SKIP_GITHUB=false
SKIP_MODRINTH=false
SKIP_CURSEFORGE=false

# ANSI only when attached to a terminal, so piping to a file or CI log stays clean.
if [[ -t 1 ]]; then
	BOLD=$'\033[1m'; DIM=$'\033[2m'; RED=$'\033[31m'; GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RESET=$'\033[0m'
else
	BOLD=""; DIM=""; RED=""; GREEN=""; YELLOW=""; RESET=""
fi

info() { printf '%b\n' "$*"; }
step() { printf '\n%s%s%s\n' "$BOLD" "$*" "$RESET"; }
ok()   { printf '  %s✓%s %s\n' "$GREEN" "$RESET" "$*"; }
warn() { printf '  %s!%s %s\n' "$YELLOW" "$RESET" "$*"; }
fail() { printf '%serror:%s %s\n' "$RED" "$RESET" "$*" >&2; exit 1; }

usage() {
	cat <<-EOF
	Edit a release's changelog once, everywhere.

	Pulls the notes from the GitHub release, opens them in your editor, and on save pushes
	the result to the GitHub release, every Modrinth version and every CurseForge file for
	that same tag.

	Usage:
	  scripts/edit-changelog.sh                 # latest release
	  scripts/edit-changelog.sh 1.0.0-b1        # a specific tag
	  scripts/edit-changelog.sh --dry-run       # show what would change, write nothing

	Credentials come from the environment or .env, using the release workflow's names:
	  PUB_MODRINTH_PROJECT_ID    PUB_MODRINTH_TOKEN
	  PUB_CURSEFORGE_PROJECT_ID  PUB_CURSEFORGE_TOKEN
	A platform whose credentials are missing is skipped with a notice.

	Options:
	  -t, --tag TAG        Release tag to edit (default: the latest release)
	  -n, --dry-run        Show what would change without writing anything
	  -y, --yes            Skip the confirmation prompt
	      --skip-github    Leave the GitHub release notes alone
	      --skip-modrinth  Leave Modrinth alone
	      --skip-curseforge
	                       Leave CurseForge alone
	  -h, --help           Show this help
	EOF
}

while [[ $# -gt 0 ]]; do
	case "$1" in
		-t|--tag) TAG="${2:-}"; shift 2 ;;
		-n|--dry-run) DRY_RUN=true; shift ;;
		-y|--yes) ASSUME_YES=true; shift ;;
		--skip-github) SKIP_GITHUB=true; shift ;;
		--skip-modrinth) SKIP_MODRINTH=true; shift ;;
		--skip-curseforge) SKIP_CURSEFORGE=true; shift ;;
		-h|--help) usage; exit 0 ;;
		-*) fail "Unknown option '$1'. Try --help." ;;
		*) TAG="$1"; shift ;;
	esac
done

for tool in gh jq curl; do
	command -v "$tool" >/dev/null || fail "'$tool' is required but not installed."
done

# Real environment variables win, so a one-off `PUB_MODRINTH_TOKEN=... script.sh` overrides .env.
if [[ -f "$ROOT/.env" ]]; then
	while IFS= read -r line || [[ -n "$line" ]]; do
		[[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
		key="${line%%=*}"; value="${line#*=}"
		key="$(printf '%s' "$key" | tr -d '[:space:]')"
		value="${value%\"}"; value="${value#\"}"
		[[ -n "${!key-}" ]] || export "$key=$value"
	done < "$ROOT/.env"

	# .env.template ships placeholders like "your_modrinth_token_here". They are non-empty, so
	# without this they would sail past the "is it configured" checks and fail as a bare 401
	# somewhere further down, which reads like a broken token rather than an unfilled one.
	for var in PUB_MODRINTH_TOKEN PUB_CURSEFORGE_TOKEN PUB_MODRINTH_PROJECT_ID PUB_CURSEFORGE_PROJECT_ID; do
		[[ "${!var-}" =~ ^your_.*_here$ ]] && unset "$var"
	done
fi

gh auth status >/dev/null 2>&1 || fail "gh is not authenticated. Run: gh auth login"

# .env.template ships PUB_DRY_RUN=true and PUB_MODRINTH_STAGING=true so that nothing local can
# touch a live listing by accident. Those flags mean the same thing here as they do to Gradle —
# a repository where "dry run" is true for the build but not for this script would be a trap.
# Both are announced rather than applied quietly, since otherwise this looks like it silently
# did nothing.
if [[ "${PUB_DRY_RUN:-}" == "true" && "$DRY_RUN" == false ]]; then
	DRY_RUN=true
	warn "PUB_DRY_RUN=true — nothing will be written. Set it to false in .env to publish."
fi

if [[ "${PUB_MODRINTH_STAGING:-}" == "true" ]]; then
	MODRINTH_API="https://staging-api.modrinth.com/v2"
	warn "PUB_MODRINTH_STAGING=true — using Modrinth's staging API, not the live site."
	if [[ "$SKIP_CURSEFORGE" == false ]]; then
		# CurseForge has no staging environment, so the build skips it entirely while staging.
		# Editing live CurseForge files from a "staging" run would defeat the point.
		warn "CurseForge skipped while staging, matching what the release build does."
		SKIP_CURSEFORGE=true
	fi
fi

if [[ -z "$TAG" ]]; then
	TAG="$(gh release view --json tagName --jq .tagName)" \
		|| fail "No releases found, and no tag given."
	info "${DIM}No tag given; using the latest release.${RESET}"
fi

gh release view "$TAG" >/dev/null 2>&1 || fail "No GitHub release found for tag '$TAG'."

step "Editing changelog for $TAG"

# ── Fetch and edit ────────────────────────────────────────────────────────────────────────────

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT
original="$workdir/original.md"
edited="$workdir/CHANGELOG.md"

gh release view "$TAG" --json body --jq .body > "$original"
cp "$original" "$edited"
ok "Pulled $(wc -l < "$original" | tr -d ' ') lines from the GitHub release."

EDITOR_CMD="${VISUAL:-${EDITOR:-}}"
if [[ -z "$EDITOR_CMD" ]]; then
	for candidate in nano vim vi; do
		command -v "$candidate" >/dev/null && { EDITOR_CMD="$candidate"; break; }
	done
fi
[[ -n "$EDITOR_CMD" ]] || fail "No editor found. Set \$EDITOR, or install nano."

info "${DIM}Opening $EDITOR_CMD — save and exit when done.${RESET}"
$EDITOR_CMD "$edited"

if cmp -s "$original" "$edited"; then
	info "\nNo changes made; nothing to publish."
	exit 0
fi
[[ -s "$edited" ]] || fail "The changelog is now empty. Refusing to publish a blank changelog."

step "Changes"
diff -u --label "before" "$original" --label "after" "$edited" || true

# ── Work out what would be touched, before asking ─────────────────────────────────────────────

modrinth_ids=""
curseforge_ids=""

if [[ "$SKIP_MODRINTH" == false ]]; then
	if [[ -n "${PUB_MODRINTH_PROJECT_ID:-}" && -n "${PUB_MODRINTH_TOKEN:-}" ]]; then
		# Platform versions are named "<tag>-<loader>+<mc>", so an exact prefix match on
		# "<tag>-" selects this release's twelve without also matching 1.0.0-b10 for 1.0.0-b1.
		modrinth_ids="$(
			curl -fsS "$MODRINTH_API/project/$PUB_MODRINTH_PROJECT_ID/version" \
				-H "Authorization: $PUB_MODRINTH_TOKEN" 2>/dev/null \
				| jq -r --arg tag "$TAG" \
					'.[] | select(.version_number == $tag or (.version_number | startswith($tag + "-"))) | .id' \
				|| true
		)"
	else
		warn "Modrinth skipped: PUB_MODRINTH_PROJECT_ID / PUB_MODRINTH_TOKEN not set."
		SKIP_MODRINTH=true
	fi
fi

if [[ "$SKIP_CURSEFORGE" == false ]]; then
	if [[ -n "${PUB_CURSEFORGE_PROJECT_ID:-}" && -n "${PUB_CURSEFORGE_TOKEN:-}" ]]; then
		# Jars are named "<mod>-<tag>-<loader>+<mc>.jar", so the surrounding dashes give the
		# same exactness the prefix match gives on Modrinth.
		curseforge_ids="$(
			curl -fsS "$CURSEFORGE_LIST_API/mods/$PUB_CURSEFORGE_PROJECT_ID/files?pageIndex=0&pageSize=200" 2>/dev/null \
				| jq -r --arg tag "$TAG" \
					'.data[]? | select(.fileName | contains("-" + $tag + "-")) | .id' \
				|| true
		)"
	else
		warn "CurseForge skipped: PUB_CURSEFORGE_PROJECT_ID / PUB_CURSEFORGE_TOKEN not set."
		SKIP_CURSEFORGE=true
	fi
fi

modrinth_count=$(printf '%s' "$modrinth_ids" | grep -c . || true)
curseforge_count=$(printf '%s' "$curseforge_ids" | grep -c . || true)

step "Will update"
[[ "$SKIP_GITHUB" == true ]]  && warn "GitHub release — skipped"     || ok "GitHub release $TAG"
[[ "$SKIP_MODRINTH" == false ]] && ok "Modrinth: $modrinth_count version(s)"
[[ "$SKIP_CURSEFORGE" == false ]] && ok "CurseForge: $curseforge_count file(s)"

if [[ "$SKIP_MODRINTH" == false && "$modrinth_count" -eq 0 ]]; then
	warn "No Modrinth versions matched '$TAG'. Has this release been published there yet?"
fi
if [[ "$SKIP_CURSEFORGE" == false && "$curseforge_count" -eq 0 ]]; then
	warn "No CurseForge files matched '$TAG'. Either it is not published there yet, or the"
	warn "unofficial listing endpoint changed — check manually before trusting this."
fi

if [[ "$DRY_RUN" == true ]]; then
	info "\n${DIM}Dry run; nothing was written.${RESET}"
	exit 0
fi

if [[ "$ASSUME_YES" == false ]]; then
	printf '\nPublish this changelog to the platforms above? [y/N] '
	read -r reply
	[[ "$reply" =~ ^[Yy]$ ]] || { info "Aborted; nothing was written."; exit 0; }
fi

# ── Publish ───────────────────────────────────────────────────────────────────────────────────

failures=0

if [[ "$SKIP_GITHUB" == false ]]; then
	step "GitHub"
	if gh release edit "$TAG" --notes-file "$edited" >/dev/null; then
		ok "Release notes updated."
	else
		warn "Failed to update the GitHub release."; failures=$((failures + 1))
	fi
fi

if [[ "$SKIP_MODRINTH" == false && "$modrinth_count" -gt 0 ]]; then
	step "Modrinth"
	payload="$(jq -n --rawfile c "$edited" '{changelog: $c}')"
	while read -r id; do
		[[ -n "$id" ]] || continue
		code="$(curl -sS -o /dev/null -w '%{http_code}' -X PATCH "$MODRINTH_API/version/$id" \
			-H "Authorization: $PUB_MODRINTH_TOKEN" \
			-H "Content-Type: application/json" \
			--data-binary "$payload")"
		if [[ "$code" == "204" ]]; then
			ok "$id"
		else
			warn "$id — HTTP $code"; failures=$((failures + 1))
		fi
	done <<< "$modrinth_ids"
fi

if [[ "$SKIP_CURSEFORGE" == false && "$curseforge_count" -gt 0 ]]; then
	step "CurseForge"
	while read -r id; do
		[[ -n "$id" ]] || continue
		metadata="$(jq -n --rawfile c "$edited" --argjson id "$id" \
			'{fileID: $id, changelog: $c, changelogType: "markdown"}')"
		code="$(curl -sS -o /dev/null -w '%{http_code}' \
			-X POST "$CURSEFORGE_UPLOAD_API/projects/$PUB_CURSEFORGE_PROJECT_ID/update-file" \
			-H "X-Api-Token: $PUB_CURSEFORGE_TOKEN" \
			-F "metadata=$metadata")"
		if [[ "$code" == "200" ]]; then
			ok "$id"
		else
			warn "$id — HTTP $code"; failures=$((failures + 1))
		fi
	done <<< "$curseforge_ids"
fi

if [[ "$failures" -gt 0 ]]; then
	printf '\n%sFinished with %d failure(s).%s The edited changelog is printed below so it is not lost:\n\n' \
		"$RED" "$failures" "$RESET" >&2
	cat "$edited" >&2
	exit 1
fi

printf '\n%sChangelog updated everywhere.%s\n' "$GREEN" "$RESET"
