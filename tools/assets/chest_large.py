"""Large Magic Chest: model and texture from the vanilla large Royal Magic chest,
recoloured the same way as the small one (chest_texture.py, chest_model.py)."""
import colorsys, json, math, random, sys
from PIL import Image
V=sys.argv[1]; OUT=sys.argv[2]
random.seed(7)
def hsv(p):
    r,g,b,a=p; h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255); return h*360,s,v,a
def rgb(h,s,v,a): r,g,b=colorsys.hsv_to_rgb((h%360)/360,max(0,min(1,s)),max(0,min(1,v))); return (round(r*255),round(g*255),round(b*255),a)

# ---------- block texture ----------
im=Image.open(f'{V}/Blocks/Decorative_Sets/Royal_Magic/Chest_Texture.png').convert('RGBA')
px=im.load(); W,H=im.size
def in_rect(x,y,r): return r[0]<=x<r[2] and r[1]<=y<r[3]
BODY=[(25,0,76,53),(0,39,76,53)]          # crimson scale panels
GEM=(0,20,14,32)
INNER=(4,56,51,76)                          # visible inside of the chest
CRYSTAL=(77,29)                             # free corner for the crystal faces
for y in range(H):
    for x in range(W):
        p=px[x,y]
        if p[3]==0: continue
        h,s,v,a=hsv(p)
        if in_rect(x,y,GEM):
            px[x,y]=rgb(180+((h-300)%360)*0.15,s*0.85,min(1,v*1.05),a)   # pink gem -> void cyan
        elif any(in_rect(x,y,r) for r in BODY):
            if 15<h<60 and s>0.5 and v>0.55:                      # orange studs -> glowing cyan
                px[x,y]=rgb(182,0.75,min(1,v*1.1),a)
            else:                                                 # crimson -> deep void violet
                px[x,y]=rgb(272+((h+40)%360-40)*0.4,min(1,s*0.95),v*0.95,a)
# void portal inside the chest, stretched to the wider opening
cx,cy=(INNER[0]+INNER[2])/2,(INNER[1]+INNER[3])/2
for y in range(INNER[1],INNER[3]):
    for x in range(INNER[0],INNER[2]):
        dx,dy=(x+0.5-cx)*0.5,y+0.5-cy; r=math.hypot(dx,dy); ang=math.atan2(dy,dx)
        swirl=0.5+0.5*math.sin(3*ang+r*0.55)
        v=0.10+0.30*swirl*max(0,1-r/14)+0.25*max(0,1-r/5)
        px[x,y]=rgb(275-r*2,0.85-0.3*max(0,1-r/5),v,255)
for _ in range(24):
    x=random.randrange(INNER[0]+1,INNER[2]-1); y=random.randrange(INNER[1]+1,INNER[3]-1)
    px[x,y]=rgb(185,0.35,1,255)
def crystal_face(ox,oy,w,h,tip):
    for j in range(h):
        for i in range(w):
            t=j/(h-1) if h>1 else 0
            edge=(i==0 or i==w-1)
            v=0.95-0.45*t-(0.15 if edge else 0)
            if tip and j==0: v=1
            px[ox+i,oy+j]=rgb(186+20*t,0.55+0.35*t,v,255)
cells=[(CRYSTAL[0]+6*(k%3),CRYSTAL[1]+6*(k//3)) for k in range(6)]
for k,(x,y) in enumerate(cells): crystal_face(x,y,5,5,k!=5)
im.save(f'{OUT}/Common/Blocks/MagicStorage/Magic_Chest_Large_Texture.png')

# ---------- model: vanilla large chest plus the floating void crystal ----------
m=json.load(open(f'{V}/Blocks/Decorative_Sets/Royal_Magic/Chest.blockymodel'))
def find(n,name,pred=lambda n:True):
    if n['name']==name and pred(n): return n
    for c in n.get('children',[]):
        r=find(c,name,pred)
        if r: return r
lid=find(m['nodes'][0],'Lid')
band=[c for c in lid['children'] if c['shape']['settings']['size']=={'x':4,'y':12,'z':28}][0]
def qmul(a,b):
    ax,ay,az,aw=a; bx,by,bz,bw=b
    return (aw*bx+ax*bw+ay*bz-az*by, aw*by-ax*bz+ay*bw+az*bx, aw*bz+ax*by-ay*bx+az*bw, aw*bw-ax*bx-ay*by-az*bz)
s45=math.sin(math.radians(22.5)); c45=math.cos(math.radians(22.5)); t=math.atan(1/math.sqrt(2))/2
x,y,z,w=qmul((0,s45,0,c45),qmul((math.sin(t),0,0,math.cos(t)),(0,0,s45,c45)))   # cube balanced on a corner
Q={"x":round(x,6),"y":round(y,6),"z":round(z,6),"w":round(w,6)}
face=lambda c:{"offset":{"x":c[0],"y":c[1]},"mirror":{"x":False,"y":False},"angle":0}
band.setdefault('children',[]).append({
  "id":"18","name":"Void_Crystal","children":[],
  "position":{"x":0,"y":11,"z":0},
  "orientation":Q,
  "shape":{"type":"box","offset":{"x":0,"y":0,"z":0},"stretch":{"x":1,"y":1,"z":1},
    "settings":{"size":{"x":5,"y":5,"z":5}},"visible":True,"doubleSided":False,
    "shadingMode":"flat","unwrapMode":"custom",
    "textureLayout":dict(zip(["front","right","back","left","top","bottom"],map(face,cells)))}})
json.dump(m,open(f'{OUT}/Common/Blocks/MagicStorage/Magic_Chest_Large.blockymodel','w'),indent=2)
