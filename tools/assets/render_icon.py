"""Tiny software renderer for .blockymodel files, used to make inventory icons."""
import json, math, sys
import numpy as np
from PIL import Image

def qmat(q):
    x,y,z,w=q['x'],q['y'],q['z'],q['w']
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],
                     [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],
                     [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])
def v3(d): return np.array([d['x'],d['y'],d['z']],float)

# face: (normal axis, sign, u axis, v axis) in box-local coords; v points down in texture
FACES={
 'front':(np.array([0,0,1.]),np.array([1,0,0.]),np.array([0,-1,0.]),('x','y')),
 'back': (np.array([0,0,-1.]),np.array([-1,0,0.]),np.array([0,-1,0.]),('x','y')),
 'right':(np.array([1,0,0.]),np.array([0,0,-1.]),np.array([0,-1,0.]),('z','y')),
 'left': (np.array([-1,0,0.]),np.array([0,0,1.]),np.array([0,-1,0.]),('z','y')),
 'top':  (np.array([0,1,0.]),np.array([1,0,0.]),np.array([0,0,1.]),('x','z')),
 'bottom':(np.array([0,-1,0.]),np.array([1,0,0.]),np.array([0,0,-1.]),('x','z')),
}

def collect(node,M,t,out):
    R=qmat(node.get('orientation',{'x':0,'y':0,'z':0,'w':1}))
    p=v3(node.get('position',{'x':0,'y':0,'z':0}))
    s=node.get('shape',{})
    off=v3(s.get('offset',{'x':0,'y':0,'z':0}))
    M2=M@R; t2=t+M@p+M2@off
    if s.get('type')=='box' and s.get('visible',True):
        st=v3(s.get('stretch',{'x':1,'y':1,'z':1})); sz=v3(s['settings']['size'])
        out.append((M2,t2,sz,st,s.get('textureLayout',{}),'box'))
    if s.get('type')=='quad' and s.get('visible',True):
        st=v3(s.get('stretch',{'x':1,'y':1,'z':1})); sz=s['settings']['size']
        out.append((M2,t2,np.array([sz['x'],sz['y'],0.]),st,s.get('textureLayout',{}),'quad'))
    for c in node.get('children',[]): collect(c,M2,t2,out)

def uv_rect(f,dims,lay):
    ox,oy=lay['offset']['x'],lay['offset']['y']; ang=lay.get('angle',0)%360
    w,h=dims
    if ang in (90,270): w,h=h,w
    return ox,oy,w,h,ang,lay['mirror']['x'],lay['mirror']['y']

def sample_uv(a,b,dims,lay):
    """a,b in [0,1) across the face (u right, v down) -> texture coords.
    The layout offset is the anchor corner; mirror flips the direction away from it and
    angle rotates the rectangle around it (clockwise, y down)."""
    ox,oy=lay['offset']['x'],lay['offset']['y']; ang=lay.get('angle',0)%360
    w,h=dims
    x=a*w*(-1 if lay['mirror']['x'] else 1); y=b*h*(-1 if lay['mirror']['y'] else 1)
    if ang==90: x,y=-y,x
    elif ang==180: x,y=-x,-y
    elif ang==270: x,y=y,-x
    return ox+x, oy+y

def render(model,tex,rot,size=256,scale=1.0,trans=(0,0),light=True):
    m=json.load(open(model)); T=np.asarray(Image.open(tex).convert('RGBA')).astype(float)
    th,tw=T.shape[:2]
    prims=[]
    for n in m['nodes']: collect(n,np.eye(3),np.zeros(3),prims)
    rx,ry,rz=[math.radians(a) for a in rot]
    Rx=np.array([[1,0,0],[0,math.cos(rx),-math.sin(rx)],[0,math.sin(rx),math.cos(rx)]])
    Ry=np.array([[math.cos(ry),0,math.sin(ry)],[0,1,0],[-math.sin(ry),0,math.cos(ry)]])
    Rz=np.array([[math.cos(rz),-math.sin(rz),0],[math.sin(rz),math.cos(rz),0],[0,0,1]])
    V=Rx@Ry@Rz if False else Rz@Rx@Ry
    quads=[]
    for M,t,sz,st,layout,kind in prims:
        half=sz*st/2
        fl = FACES.items() if kind=='box' else [('front',FACES['front'])]
        for name,(nrm,u,vv,dn) in fl:
            if name not in layout: continue
            dims=(abs(sz['xyz'.index(dn[0])]),abs(sz['xyz'.index(dn[1])]))
            c=nrm*half; U=u*half; Vd=vv*half
            if kind=='quad': c=np.zeros(3)
            corner=c-U-Vd   # top-left
            P0=V@(M@corner+t); PU=V@(M@(2*U)); PV=V@(M@(2*Vd))
            N=V@(M@(nrm*np.sign(st)))*(-1 if np.prod(np.sign(st))<0 else 1)  # inverted boxes show their inside
            quads.append((P0,PU,PV,dims,layout[name],N,kind))
    # bounds -> fit
    img=np.zeros((size,size,4)); zb=np.full((size,size),-1e9)
    pts=np.array([q[0]+a*q[1]+b*q[2] for q in quads for a in (0,1) for b in (0,1)])
    lo,hi=pts[:,:2].min(0),pts[:,:2].max(0); ext=(hi-lo).max()
    S=size*0.9/ext*scale; mid=(lo+hi)/2
    cx,cy=size/2-mid[0]*S+trans[0]*size/64, size/2+mid[1]*S-trans[1]*size/64
    ys,xs=np.mgrid[0:size,0:size]
    for P0,PU,PV,dims,lay,N,kind in quads:
        if kind=='box' and N[2]<=1e-6: continue
        A=np.array([[PU[0],PV[0]],[PU[1],PV[1]]])
        if abs(np.linalg.det(A))<1e-9: continue
        Ai=np.linalg.inv(A)
        wx=(xs+0.5-cx)/S-P0[0]; wy=-(ys+0.5-cy)/S-P0[1]
        a=Ai[0,0]*wx+Ai[0,1]*wy; b=Ai[1,0]*wx+Ai[1,1]*wy
        mask=(a>=0)&(a<1)&(b>=0)&(b<1)
        if not mask.any(): continue
        z=P0[2]+a*PU[2]+b*PV[2]
        tx,ty=sample_uv(a,b,dims,lay)
        txi=np.clip(np.floor(tx).astype(int),0,tw-1); tyi=np.clip(np.floor(ty).astype(int),0,th-1)
        col=T[tyi,txi]
        mask&=col[...,3]>0
        mask&=z>zb
        n=N/np.linalg.norm(N)
        shade=0.72+0.38*max(0,np.dot(n,np.array([-0.3,0.85,0.45])/np.linalg.norm([-0.3,0.85,0.45]))) if light else 1
        img[mask]=np.concatenate([col[mask][:,:3]*shade,col[mask][:,3:]],axis=1)
        zb[mask]=z[mask]
    return Image.fromarray(img.clip(0,255).astype('uint8'),'RGBA')

if __name__=='__main__':
    model,tex,out=sys.argv[1:4]
    rot=tuple(map(float,sys.argv[4].split(','))) if len(sys.argv)>4 else (22.5,45,22.5)
    scale=float(sys.argv[5]) if len(sys.argv)>5 else 1
    tr=tuple(map(float,sys.argv[6].split(','))) if len(sys.argv)>6 else (0,0)
    im=render(model,tex,rot,256,scale,tr)
    im.resize((64,64),Image.LANCZOS).save(out)
    im.save(out.replace('.png','_256.png'))
