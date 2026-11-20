# Browser, recents and favorites

The home screen has a top-left hamburger setup menu and Recents, Favorites and Open buttons. Recents/Favorites browse `Internal storage/infini/play/recents/<system>/` and `Internal storage/infini/play/favorites/<system>/`; their top levels list systems. System folder names are lowercase enum IDs, e.g. `nes`, `sg1000`, `mastersystem`, `megadrive`. Open starts at Virtual root. The embedded InfiniSelect View hosts Folder's complete browser surface. Folder owns its file list, breadcrumbs, directory navigation, gestures, file menus and thumbnails in its own process; Play and InfiniSelect do not render or maintain a second file viewer.

Selecting a game immediately stages necessary core working content and starts playback after missing firmware/core prompts. Extensions and bounded cartridge signatures detect known systems. Ambiguous files request a console once, retained in link metadata; there is no persistent system chooser. ACTION_VIEW and single-file ACTION_SEND use the same .iflink resolution, import/deduplication, recent-link and launch pipeline. Core choices persist per system.

The old private library is seeded into shared recent links on first collection setup; game IDs, working copies and saves are kept.

Opened sources get version-1 `.iflink` files in their system's recent directory. Repeated opens reuse the same source link and update its modification time. Links target original authoritative resources rather than expiring access relays; they do not contain ROM data. Existing user-named links are reused too. Long-press any game file or game shortcut for **Open in emulator**, including ISO images and ZIP archives. This returns the whole selected file to Play's normal source-resolution, system/core selection, recent-link, firmware and launch pipeline. A normal tap on a browsable container still enters it in Folder. The long-press menu also offers Add to favorites, or Remove favorite on the Favorites page. InfiniPlay supplies these actions through the reusable InfiniSelect `fileActions`/`onFileAction` hooks; Folder renders the menu and the gateway returns the action ID. The floating game picker receives the same Open in emulator and Add to favorites actions. The firmware picker offers **Use as firmware**, so a BIOS ZIP or disk-image filename can be selected as a whole file without entering it. Firmware selection continues through size/archive validation; it never launches as a game. Favorites actions only read/write the link; they do not launch, import or require access to the game target. Removing a favorite deletes only its link. Cyclic, oversized or over-deep links fail visibly.

InfiniPlay declares no shared-storage or all-files permissions and shows no storage permission prompt. InfiniSelect embeds Folder and requests collection writes from it, which uses its existing access. No Folder tree grant is required. The verified caller package scopes writes to infini/play. If Folder is absent, Android file selection/granted SAF sources remain available and collections are app-private; they switch to shared storage when Folder is available. An old installed Folder must be upgraded to support this API.

The breadcrumb trail and list/icon toggle are Folder's actual toolbar, shared with its floating selector. Setup asks Folder to hide/show the toolbar. Android Back forwards to Folder when a parent is available. Folder handles clickable ancestors, source changes, archives, link resolution, pull-to-refresh and pinch sizing. In-app embedding requires updated Folder on Android 11+; otherwise the gateway offers ordinary InfiniSelect/Android selection. No substitute directory UI is constructed.

Tests cover system dispatch and durable link deduplication. Touch UI, directory relay/paging and selected-file grants, incoming grants and firmware-assisted immediate launch need device validation.

Future caller actions can look up extended metadata, box art or thumbnails through the same hook. No online metadata/artwork service is implemented in this change.

InfiniPlay 0.2.4 pins InfiniSelect 0.2.1. Its embedded callback crosses the app
boundary as Android's plain ResultReceiver, preventing Folder from trying to
load a private InfiniPlay callback class during Bundle unmarshalling. Protocol
version 1 and Folder-owned browsing remain unchanged.

In 0.2.5, Recents and Favorites set InfiniSelect's `lockToDirectory` option for
their collection directories. Folder keeps breadcrumbs, Back and restored
surface state inside that navigation root, including load-error recovery. Open
clears the lock and starts at Virtual root. The gateway places the embedded
surface above the host window for real touch input, and Folder removes its
floating-frame elevation/insets when embedded.

Version 0.2.6 uses the general .iflink model from InfiniSelect 0.2.3. Links can
point to directories, network resources and arbitrary absolute URIs. Folder
resolves targets before routing them, wherever the shortcut is stored. A regular
game target can still return its original .iflink to preserve system hints. A
directory link delivered directly to Play opens the Folder-owned browser at
that directory. Directory targets do not enter Play's game Favorites workflow.
URI handlers and all browsing remain Folder responsibilities.

Version 0.2.7 pins InfiniSelect 0.2.4 so .iflink targets with legacy raw spaces
or other Android resource path characters survive parsing and JSON round trips.
Folder 0.1.12 emits correctly encoded SFTP URIs for newly listed files. Display
filenames keep their spaces; URI serialization and resource access stay distinct.

Version 0.2.8 adds these explicit whole-file actions using the existing selector
contract; it does not change Folder or introduce a second file browser. ZIPs
remain intact when staged. The selected core must support their contents:
Geolith still requires `.neo` Neo Geo cartridges, and this change does not add
MAME/FinalBurn ZIP ROM-set emulation.
