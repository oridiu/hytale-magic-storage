import json, sys
V=sys.argv[1]; OUT=sys.argv[2]
m=json.load(open(f'{V}/Blocks/Decorative_Sets/Royal_Magic/Chest_Small.blockymodel'))
def find(n,name,pred=lambda n:True):
    if n['name']==name and pred(n): return n
    for c in n.get('children',[]):
        r=find(c,name,pred)
        if r: return r
lid=find(m['nodes'][0],'Lid')
band=[c for c in lid['children'] if c['shape']['settings']['size']=={'x':6,'y':13,'z':28}][0]
import math
def qmul(a,b):
    ax,ay,az,aw=a; bx,by,bz,bw=b
    return (aw*bx+ax*bw+ay*bz-az*by, aw*by-ax*bz+ay*bw+az*bx, aw*bz+ax*by-ay*bx+az*bw, aw*bw-ax*bx-ay*by-az*bz)
s45=math.sin(math.radians(22.5)); c45=math.cos(math.radians(22.5)); t=math.atan(1/math.sqrt(2))/2
x,y,z,w=qmul((0,s45,0,c45),qmul((math.sin(t),0,0,math.cos(t)),(0,0,s45,c45)))   # cube balanced on a corner
Q={"x":round(x,6),"y":round(y,6),"z":round(z,6),"w":round(w,6)}
face=lambda x,y:{"offset":{"x":x,"y":y},"mirror":{"x":False,"y":False},"angle":0}
band['children'].append({
  "id":"28","name":"Void_Crystal","children":[],
  "position":{"x":0,"y":11.5,"z":0},
  "orientation":Q,
  "shape":{"type":"box","offset":{"x":0,"y":0,"z":0},"stretch":{"x":1,"y":1,"z":1},
    "settings":{"size":{"x":5,"y":5,"z":5}},"visible":True,"doubleSided":False,
    "shadingMode":"flat","unwrapMode":"custom",
    "textureLayout":{"front":face(1,1),"right":face(7,1),"back":face(13,1),"left":face(19,1),"top":face(1,7),"bottom":face(7,7)}}})
json.dump(m,open(f'{OUT}/Common/Blocks/MagicStorage/Magic_Chest.blockymodel','w'),indent=2)
