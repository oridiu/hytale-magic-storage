import colorsys, math, random, sys
from PIL import Image
V=sys.argv[1]; OUT=sys.argv[2]
random.seed(7)
def hsv(p):
    r,g,b,a=p; h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255); return h*360,s,v,a
def rgb(h,s,v,a): r,g,b=colorsys.hsv_to_rgb((h%360)/360,max(0,min(1,s)),max(0,min(1,v))); return (round(r*255),round(g*255),round(b*255),a)

# ---------- block texture ----------
im=Image.open(f'{V}/Blocks/Decorative_Sets/Royal_Magic/Chest_Small_Texture.png').convert('RGBA')
px=im.load(); W,H=im.size
def in_rect(x,y,r): return r[0]<=x<r[2] and r[1]<=y<r[3]
BODY=[(25,0,55,53),(0,39,55,53)]          # crimson scale panels
GEM=(1,16,13,28)
INNER=(57,1,85,25)                          # visible inside of the chest
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
# void portal inside the chest
cx,cy=(INNER[0]+INNER[2])/2,(INNER[1]+INNER[3])/2
for y in range(INNER[1],INNER[3]):
    for x in range(INNER[0],INNER[2]):
        dx,dy=x+0.5-cx,y+0.5-cy; r=math.hypot(dx,dy); ang=math.atan2(dy,dx)
        swirl=0.5+0.5*math.sin(3*ang+r*0.55)
        v=0.10+0.30*swirl*max(0,1-r/14)+0.25*max(0,1-r/5)
        px[x,y]=rgb(275-r*2,0.85-0.3*max(0,1-r/5),v,255)
for _ in range(14):
    x=random.randrange(INNER[0]+1,INNER[2]-1); y=random.randrange(INNER[1]+1,INNER[3]-1)
    px[x,y]=rgb(185,0.35,1,255)
# void crystal faces in the free top-left corner (used by the new Void_Crystal node)
def crystal_face(ox,oy,w,h,tip):
    for j in range(h):
        for i in range(w):
            t=j/(h-1) if h>1 else 0
            edge=(i==0 or i==w-1)
            v=0.95-0.45*t-(0.15 if edge else 0)
            if tip and j==0: v=1
            px[ox+i,oy+j]=rgb(186+20*t,0.55+0.35*t,v,255)
for k in range(4): crystal_face(1+6*k,1,5,5,True)
crystal_face(1,7,5,5,True); crystal_face(7,7,5,5,False)
im.save(f'{OUT}/Common/Blocks/MagicStorage/Magic_Chest_Texture.png')

