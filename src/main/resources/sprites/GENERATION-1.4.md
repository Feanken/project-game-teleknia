# Version 1.4 sprite atlas

Generated using the built-in imagegen tool. Sixteen independent source sprites
were extracted from a 4x4 atlas and resized to exactly 64x64 with nearest-neighbor
interpolation. Runtime rendering uses integer scales with smoothing disabled.

Final generation prompt:

Create ONE square 4x4 production pixel-art RPG sprite atlas, exactly sixteen uniformly sized square cells with NO grid lines, no padding between cells, no text or labels. Each cell contains independent 64x64 pixel art using hard pixel clusters, crisp square edges, detailed handcrafted 16-bit dark-fantasy game style. Flat background color #101820 in every cell. Six pixels safe margin inside each cell. No overlaps across cells. Limited midnight navy, jade, pale gold, icy blue and ember palette, consistent with a retro fantasy RPG. Exact row-major cell contents: ROW1 four bust portraits: female mountain scout Sera wearing fur hood and orange scarf; elderly monk Orin with white beard and icy blue robes; weathered sailor Marin with dark skin and teal bandana; female lighthouse keeper Ilyra with silver hair and lantern. ROW2: full-body armored ice sentinel boss holding greatsword; ghost pirate captain boss with spectral anchor; snowy mountain pass landscape; ancient ice shrine landscape. ROW3: stormy drowned harbor coastline landscape; lighthouse on dark rocky cliffs landscape; golden signal lens icon; carved blue runic tablet icon. ROW4: wooden caravan supply crate icon; sealed medicine satchel icon; rolled nautical star chart icon; silver bell wrapped in seaweed icon. All sixteen cells perfectly aligned in equal 4x4 grid. Readable silhouettes, no smooth gradients, no antialiasing, no words. Landscapes are square scenic vignettes; all other cells portrait or item sprites on flat background.


## Field atlas

A second built-in imagegen atlas provides three class figures, the three original
encounters, seven original location scenes, and three equipment sprites. All were
extracted into native 64x64 PNGs with nearest-neighbor resampling.

Prompt:

One production sprite atlas for a retro dark fantasy RPG: square image, exact uniform 4x4 grid of 16 square independent cells, no gaps, no labels, no text, no grid outlines. Every cell a 64x64 pixel-art sprite with hard pixel clusters, no antialiasing, navy #101820 flat background, limited jade / midnight blue / pale gold / ember palette with icy blue accents. Each character full body in center with 6 pixel safe margin, clear at 64px. Row1: full body young male fighter with dark hair teal scarf and sword; hooded rogue with twin daggers; dark-haired mage with glowing staff; monstrous gray ash wolf with ember eyes. Row2: hollow armored guardian knight with broken sun shield; Cinder Warden black armored boss with flaming crown; scorched forest trail landscape; burned village landscape. Row3: ruined northern stone gate landscape; survivor camp with tents and warm campfire landscape; mysterious forest sun shrine landscape; flooded ancient stone library ruins landscape. Row4: dark tower with ember glow landscape; crystalline frost sword item icon diagonal; icy blue scale mail armor item icon; silver harbor oath ring item icon. Deliberate retro game pixel-art shading, readable silhouettes, all contents contained strictly within their own cells. Landscape cells are independent square scenic vignettes. All rows and columns perfectly even. No blur no typography.
