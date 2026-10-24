import colorsys, math, random, sys, json
from PIL import Image
V=sys.argv[1]; OUT=sys.argv[2]
random.seed(11)
def hsv(p):
    r,g,b,a=p; h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255); return h*360,s,v,a
def rgb(h,s,v,a=255): r,g,b=colorsys.hsv_to_rgb((h%360)/360,max(0,min(1,s)),max(0,min(1,v))); return (round(r*255),round(g*255),round(b*255),a)
def inr(x,y,r): return r[0]<=x<r[2] and r[1]<=y<r[3]

im=Image.open(f'{V}/Items/Tools/Feedbag/Fertilizer_Crystal.png').convert('RGBA'); px=im.load()
TRIM=[(0,16,42,26),(0,0,24,16)]         # Bag3 rim band (front/back + sides) and its top frame
CONTENT=[(24,0,48,16)]                  # Bag-Inner floor seen from above
CRYSTAL=[(48,0,60,18)]                  # "Feed" box, now a void crystal sticking out
EMBLEM=(2,33,20,52)
for y in range(64):
    for x in range(64):
        p=px[x,y]
        if p[3]==0: continue
        h,s,v,a=hsv(p)
        if any(inr(x,y,r) for r in CONTENT):
            continue
        if any(inr(x,y,r) for r in CRYSTAL):
            continue
        if any(inr(x,y,r) for r in TRIM):
            px[x,y]=rgb(40-(1-v)*14,0.72+(1-v)*0.15,0.18+v*0.78,a)        # gold rim
        elif inr(x,y,EMBLEM) and s<0.25:
            px[x,y]=rgb(262,0.55,v*0.45,a)                                 # erase vanilla crystal shards
        else:
            px[x,y]=rgb(262+(h-255)*0.5,min(1,s*1.15),v*0.82,a)            # deeper void violet cloth
# void inside the bag
for y in range(0,16):
    for x in range(24,48):
        if px[x,y][3]==0: continue
        dx,dy=x-35.5,y-7.5; r=math.hypot(dx,dy*1.4); ang=math.atan2(dy,dx)
        sw=0.5+0.5*math.sin(3*ang+r*0.6)
        px[x,y]=rgb(272-r*2,0.85,0.10+0.28*sw*max(0,1-r/14)+0.25*max(0,1-r/4))
for _ in range(10):
    x=random.randrange(25,47); y=random.randrange(1,15)
    if px[x,y][3]: px[x,y]=rgb(186,0.35,1)
# crystal faces
for y in range(0,18):
    for x in range(48,60):
        if px[x,y][3]==0: continue
        t=(y%9)/8; edge=(x in (48,59,51))
        px[x,y]=rgb(184+22*t,0.45+0.4*t,(1.0-0.45*t)-(0.12 if edge else 0))
# emblem: gold-framed eye/diamond rune on the front
cx,cy=11,42
for y in range(EMBLEM[1],EMBLEM[3]):
    for x in range(EMBLEM[0],EMBLEM[2]):
        d=abs(x+0.5-cx)+abs(y+0.5-cy)*1.25
        if d<3.2: px[x,y]=rgb(186,0.45+0.1*d,1-0.08*d)
        elif d<4.6: px[x,y]=rgb(272,0.8,0.25)
        elif d<6.0: px[x,y]=rgb(44,0.75,0.95-0.1*(d-4.6))
px[cx-1,cy-1]=rgb(186,0.1,1)
im.save(f'{OUT}/Common/Items/MagicStorage/Magic_Bag_Texture.png')

m=json.load(open(f'{V}/Items/Tools/Feedbag/Feedbag.blockymodel'))
def ren(n):
    if n['name']=='Feed': n['name']='Void_Crystal'
    for c in n.get('children',[]): ren(c)
for n in m['nodes']: ren(n)
json.dump(m,open(f'{OUT}/Common/Items/MagicStorage/Magic_Bag.blockymodel','w'),indent=2)
