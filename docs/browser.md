# Browser, recents and favorites

The home screen has a top-left hamburger setup menu and Recents, Favorites and Open buttons. Recents/Favorites browse `Internal storage/infini/play/recents/<system>/` and `Internal storage/infini/play/favorites/<system>/`; their top levels list systems. System folder names are lowercase enum IDs, e.g. `nes`, `sg1000`, `mastersystem`, `megadrive`. Open starts at Virtual root. The embedded libinfiniselect View browses local files and granted SAF trees, including InfiniFolder's public DocumentsProvider sources. No other application's source or private storage is embedded.

Selecting a game immediately stages necessary core working content and starts playback after missing firmware/core prompts. Extensions and bounded cartridge signatures detect known systems. Ambiguous files request a console once, retained in link metadata; there is no persistent system chooser. ACTION_VIEW and single-file ACTION_SEND use the same .iflink resolution, import/deduplication, recent-link and launch pipeline. Core choices persist per system.

The old private library is seeded into shared recent links on first collection setup; game IDs, working copies and saves are kept.

Opened sources get version-1 `.iflink` files in their system's recent directory. Repeated opens reuse the same source link and update its modification time. Links target original authoritative resources rather than expiring access relays; they do not contain ROM data. Existing user-named links are reused too. Long-press a file in Recents for Add to favorites, or in Favorites for Remove favorite. InfiniPlay supplies these actions through the reusable InfiniSelect `fileActions`/`onFileAction` hooks; the selector only renders and dispatches them. The floating game picker receives the same caller-defined Add to favorites action. Favorites actions only read/write the link; they do not launch, import or require access to the game target. Removing a favorite deletes only its link. Cyclic, oversized or over-deep links fail visibly.

Android 11+ needs user-granted all-files access for these exact shared-storage paths; older Android requests shared-storage read/write permission. A first-use explanation returns to the pending operation after permission is granted. Denial does not redirect links to a hidden alternate directory. Folder listing separately uses a normal tree grant. Folder remains optional; Android file selection, local files and other granted storage trees remain available.

The breadcrumb bar has Back, path and a persistent list/icon toggle. Setup can hide the entire bar and its toggle. Android Back still navigates within the selected root. Folder's floating selector has a separate toggle beside its breadcrumbs with its own stored preference. The embedded component uses basic symbols; Folder's complete thumbnails, themes and file-operation UI are not embedded in this initial component.

Tests cover system dispatch and durable link deduplication. Touch UI, storage/tree permission transitions, incoming grants and firmware-assisted immediate launch need device validation.

Future caller actions can look up extended metadata, box art or thumbnails through the same hook. No online metadata/artwork service is implemented in this change.
