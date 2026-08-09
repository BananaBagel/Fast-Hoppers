#!/usr/bin/env bash
#
# Runs Gradle, retrying only when the failure was a repository that did not answer.
#
# Dependency resolution reaches several small self-hosted mavens, and Gradle treats a transport
# failure as fatal rather than falling through to the next repository — one unanswered request
# fails the whole job. Retrying costs almost nothing because the second attempt runs against a
# warm cache.
#
# The output is matched so that a real failure — a compile error, a failing gametest — is reported
# on the first attempt instead of being run three times.
#
# Usage: bash .github/scripts/gradle-retry.sh <gradle args...>

set -uo pipefail

ATTEMPTS="${GRADLE_RETRY_ATTEMPTS:-3}"

# Transport-level signatures only. Deliberately excludes the generic "Could not resolve all files"
# line, which is also what a genuinely missing dependency produces.
RETRYABLE='failed to respond|Could not GET|Could not get resource|Could not download|Read timed out|Connect timed out|Connection reset|Premature end of Content-Length|Remote host (closed|terminated)|HTTP 50[234]|Gateway Time-out'

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

for attempt in $(seq 1 "$ATTEMPTS"); do
	if ./gradlew "$@" 2>&1 | tee "$log"; then
		exit 0
	fi

	if ! grep -qE "$RETRYABLE" "$log"; then
		echo "::error::Gradle failed for a non-network reason; not retrying."
		exit 1
	fi

	if [[ "$attempt" -eq "$ATTEMPTS" ]]; then
		echo "::error::A repository was still unreachable after ${ATTEMPTS} attempts."
		exit 1
	fi

	delay=$((attempt * 30))
	echo "::warning::Attempt ${attempt} failed reaching a repository; retrying in ${delay}s."
	sleep "$delay"
done
