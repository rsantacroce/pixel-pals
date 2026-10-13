import os, struct, zlib, subprocess, shutil, sys
root = sys.argv[1]
ART = [
"................",
".r............b.",
"................",
"....kkkkkkkk....",
"...kyyyyyyyyk...",
"..kyywyyyyyyyk..",
"..kyykyyyykyyk..",
"..kyykyyyykyyk..",
"..kyyyyyyyyyyk..",
"..kpyyyyyyyypk..",
"..kyykyyyykyyk..",
"..kyyykkkkyyyk..",
"...kyyyyyyyyk...",
"....kkkkkkkk....",
".g............o.",
"................",
]
COL = {'k':0x222034,'y':0xFFD93D,'w':0xFFFFFF,'p':0xFF77A8,'r':0xE8263B,'b':0x3A7BFF,'g':0x3EC74B,'o':0xFF8C1A}
BG = 0x5B3CF5

def png(size, pad_frac, rounded):
    cell = size * (1 - 2*pad_frac) / 16
    off = size * pad_frac
    rows = []
    radius = size * 0.2
    for y in range(size):
        row = bytearray([0])
        for x in range(size):
            a = 255
            if rounded:
                cx = min(max(x, radius), size-1-radius); cy = min(max(y, radius), size-1-radius)
                if (x-cx)**2 + (y-cy)**2 > radius**2: a = 0
            c = BG
            gx = int((x - off)//cell); gy = int((y - off)//cell)
            if 0 <= gx < 16 and 0 <= gy < 16 and ART[gy][gx] != '.':
                c = COL[ART[gy][gx]]
            row += bytes([(c>>16)&255,(c>>8)&255,c&255,a])
        rows.append(bytes(row))
    raw = b''.join(rows)
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t+d) & 0xffffffff)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')

icons = os.path.join(root, 'desktopApp/icons'); os.makedirs(icons, exist_ok=True)
open(os.path.join(icons, 'icon.png'), 'wb').write(png(512, 0.08, True))
# ICO with embedded PNGs
sizes = [16, 32, 48, 256]
imgs = [png(s, 0.04, s >= 48) for s in sizes]
hdr = struct.pack('<HHH', 0, 1, len(sizes)); offset = 6 + 16*len(sizes); entries = b''
for s, d in zip(sizes, imgs):
    entries += struct.pack('<BBBBHHII', s % 256, s % 256, 0, 0, 1, 32, len(d), offset); offset += len(d)
open(os.path.join(icons, 'icon.ico'), 'wb').write(hdr + entries + b''.join(imgs))
# ICNS via iconutil when available (macOS)
iconset = os.path.join(os.environ.get('TMPDIR', '/tmp'), 'pixelpals.iconset')
shutil.rmtree(iconset, ignore_errors=True); os.makedirs(iconset)
for s in [16, 32, 128, 256, 512]:
    open(os.path.join(iconset, f'icon_{s}x{s}.png'), 'wb').write(png(s, 0.08, True))
    open(os.path.join(iconset, f'icon_{s}x{s}@2x.png'), 'wb').write(png(s*2, 0.08, True))
if shutil.which('iconutil'):
    subprocess.run(['iconutil', '-c', 'icns', iconset, '-o', os.path.join(icons, 'icon.icns')], check=True)

# Android adaptive icon: vector foreground (108x108 viewport, art in the 66dp safe zone)
res = os.path.join(root, 'androidApp/src/main/res')
for d in ['drawable', 'mipmap-anydpi-v26', 'values', 'xml']: os.makedirs(os.path.join(res, d), exist_ok=True)
paths = []
scale = 64/16; base = 22
for color in COL:
    d = ''
    for y in range(16):
        for x in range(16):
            if ART[y][x] == color:
                px, py = base + x*scale, base + y*scale
                d += f'M{px:g},{py:g}h{scale:g}v{scale:g}h-{scale:g}z'
    if d: paths.append(f'    <path android:fillColor="#FF{COL[color]:06X}" android:pathData="{d}"/>')
open(os.path.join(res, 'drawable/ic_launcher_foreground.xml'), 'w').write(
'<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp" android:height="108dp"\n    android:viewportWidth="108" android:viewportHeight="108">\n' + '\n'.join(paths) + '\n</vector>\n')
open(os.path.join(res, 'values/ic_launcher_background.xml'), 'w').write(
f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#FF{BG:06X}</color>\n</resources>\n')
adaptive = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/ic_launcher_background"/>\n    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n    <monochrome android:drawable="@drawable/ic_launcher_foreground"/>\n</adaptive-icon>\n'
for n in ['ic_launcher.xml', 'ic_launcher_round.xml']:
    open(os.path.join(res, 'mipmap-anydpi-v26', n), 'w').write(adaptive)
print('icons written')
