<a href="https://modrinth.com/mod/Zs79Mkhy">
<img src="src/main/resources/assets/fasthoppers/icon.png" width=128 alt="Fast Hoppers icon" />
</a>

# Fast Hoppers

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/Zs79Mkhy?logo=modrinth)](https://modrinth.com/mod/Zs79Mkhy)
[![Curseforge Downloads](https://img.shields.io/curseforge/dt/912142?logo=curseforge&label=Downloads)](https://www.curseforge.com/minecraft/mc-mods/fast-hoppers)
[![Modrinth Version](https://img.shields.io/modrinth/v/Zs79Mkhy?logo=Modrinth&label=Latest%20Version&color=14a551)](https://modrinth.com/mod/Zs79Mkhy)
[![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/Zs79Mkhy?style=flat&logo=Modrinth&label=Supported%20Versions&color=%2314a551)](https://modrinth.com/mod/Zs79Mkhy)

make hoppers go *vroom vrooooooom*

## How to use

Fast Hoppers is server-side mod. Put it on your server, or client for
singleplayer, and everything works — vanilla clients can join without installing
anything.

Once you have the [mod](https://modrinth.com/mod/Zs79Mkhy) installed, go into
any world (existing or new) and you can change hopper speed with the new
gamerule:

|         1.19.2-1.21.1         |                   1.21.11+                   |
|:-----------------------------:|:--------------------------------------------:|
|     `hopperTransferDelay`     |     `fasthoppers:hopper_transfer_delay`      |
| `hopperMinecartTransferDelay` | `fasthoppers:hopper_minecart_transfer_delay` |

From 1.21.11 gamerules are namespaced, so the `fasthoppers:` prefix is
required — the bare name won't autocomplete or run.

## What does a "transfer" mean?

Normally, a hopper will move one item to the next container every 8 ticks, or 2½
items per second. With this mod, you can change the tick cooldown of your
hoppers with the gamerule command.

## Configuration

`/gamerule hopperTransferDelay <number, default 8>` (1.19.2-1.21.1)
<br>
`/gamerule fasthoppers:hopper_transfer_delay <number, default 8>` (1.21.11+)
<br>
The number you set is the number of ticks it takes for a hopper to transfer an
item. The default is 8, which is the vanilla value. Setting it to 1 will make
hoppers transfer items every tick, or 20 times per second.
<br><br>

`/gamerule hopperMinecartTransferDelay <number, default 1>` (1.19.2-1.21.1)
<br>
`/gamerule fasthoppers:hopper_minecart_transfer_delay <number, default 1>`
(1.21.11+)
<br>
The number you set is the number of ticks it takes for a hopper minecart to
transfer an item. The default is 1, which is the vanilla value on 1.20.1 and up.
This setting exists for setting slower values (such as 20, aka 1 item per
second).
<br><br>
On 1.19.2 vanilla was inconsistent here — a hopper minecart picked up every 4
ticks when sitting still, but every tick while moving. This mod makes the
gamerule apply either way, so 1.19.2 behaves like every later version.

## Supported Versions

|                                                                     Fast Hoppers | 1.19.2 | 1.20.1 | 1.21.1 | 1.21.11 | 26.1.x | 26.2 |
|---------------------------------------------------------------------------------:|:------:|:------:|:------:|:-------:|:------:|:----:|
|       **Fabric** <img src="assets/fabric.svg" alt="Fabric" style="width:20px;"/> |   ✅   |   ✅   |   ✅   |   ✅    |   ☑️   |  ✅  |
| **NeoForge** <img src="assets/neoforge.png" alt="NeoForge" style="width:20px;"/> |   ❌   |   ❌   |   ✅   |   ✅    |   ☑️   |  ✅  |
|          **Forge** <img src="assets/forge.svg" alt="Forge" style="width:20px;"/> |   ✅   |   ✅   |   ❌   |   ❌    |   ❌   |  ❌  |

✅ = supported,
<br>
☑️ = supported, but not for long...
<br>
⭕ = twas once supported, but those days are long gone...
<br>
❌ = unsupported

## Can I use it in a modpack?

Yes. I ask that you please credit the mod in your modpack description, such as a
link to the [mod page](https://modrinth.com/mod/Zs79Mkhy), but you CAN decide to
be poopy and not do that. The Bagel would be sad though :(

## Why updates now?

I, Bagel the Baddy, am on the spectrum of a thing called the 'tism and I really
wanted to re-up this mod because I felt like it. It's now made
with [Stonecutter](https://stonecutter.kikugie.dev/)
to make multi-version and multi-loader support easier. I also wanted to make it
better, with all the new knowledge & experience I have gained in the 3 years
since I first created the mod.

## Plans for the future

* MULTIPLE ITEMS PER TRANSFER (instead of 1 item at a time, maybe like 64... who
  knows...)

___
***To those who understand, I lost the game.***
