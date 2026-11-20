# Browser, recents and favorites

The home screen has a top-left hamburger setup menu and Recents, Favorites and Open buttons. Recents/Favorites browse `Internal storage/infini/play/recents/<system>/` and `Internal storage/infini/play/favorites/<system>/`; their top levels list systems. System folder names are lowercase enum IDs, e.g. `nes`, `sg1000`, `mastersystem`, `megadrive`. Open starts at Virtual root. The embedded InfiniSelect View hosts Folder's complete browser surface. Folder owns its file list, breadcrumbs, directory navigation, gestures, file menus and thumbnails in its own process; Play and InfiniSelect do not render or maintain a second file viewer.

Selecting a game immediately stages necessary core working content and starts playback after missing firmware/core prompts. Extensions and bounded cartridge signatures detect known systems. Ambiguous files request a console once, retained in link metadata; there is no persistent system chooser. ACTION_VIEW and single-file ACTION_SEND use the same .iflink resolution, import/deduplication, recent-link and launch pipeline. Core choices persist per system.

The old private library is seeded into shared recent links on first collection setup; game IDs, working copies and saves are kept.

Opened sources get version-1 `.iflink` files in their system's recent directory. Repeated opens reuse the same source link and update its modification time. Links target original authoritative resources rather than expiring access relays; they do not contain ROM data. Existing user-named links are reused too. Long-press a file in Recents for Add to favorites, or in Favorites for Remove favorite. InfiniPlay supplies these actions through the reusable InfiniSelect `fileActions`/`onFileAction` hooks; the selector only renders and dispatches them. The floating game picker receives the same caller-defined Add to favorites action. Favorites actions only read/write the link; they do not launch, import or require access to the game target. Removing a favorite deletes only its link. Cyclic, oversized or over-deep links fail visibly.

InfiniPlay declares no shared-storage or all-files permissions and shows no storage permission prompt. InfiniSelect embeds Folder and requests collection writes from it, which uses its existing access. No Folder tree grant is required. The verified caller package scopes writes to infini/play. If Folder is absent, Android file selection/granted SAF sources remain available and collections are app-private; they switch to shared storage when Folder is available. An old installed Folder must be upgraded to support this API.

The breadcrumb trail and list/icon toggle are Folder's actual toolbar, shared with its floating selector. Setup asks Folder to hide/show the toolbar. Android Back forwards to Folder when a parent is available. Folder handles clickable ancestors, source changes, archives, link resolution, pull-to-refresh and pinch sizing. In-app embedding requires updated Folder on Android 11+; otherwise the gateway offers ordinary InfiniSelect/Android selection. No substitute directory UI is constructed.

Tests cover system dispatch and durable link deduplication. Touch UI, directory relay/paging and selected-file grants, incoming grants and firmware-assisted immediate launch need device validation.

Future caller actions can look up extended metadata, box art or thumbnails through the same hook. No online metadata/artwork service is implemented in this change.

InfiniPlay 0.2.4 pins InfiniSelect 0.2.1. Its embedded callback crosses the app
boundary as Android's plain ResultReceiver, preventing Folder from trying to
load a private InfiniPlay callback class during Bundle unmarshalling. Protocol
version 1 and Folder-owned browsing remain unchanged.
