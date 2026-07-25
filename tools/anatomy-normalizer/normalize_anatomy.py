#!/usr/bin/env python3

"""
RepLog Anatomy SVG Normaliser

Normalises anatomy SVGs so every illustration:

• fits a 768×1536 viewBox
• is centred
• occupies ~93% of the canvas
• keeps original colours
• preserves path transforms

Usage

python normalize_anatomy.py input_folder output_folder

"""

import copy
import math
import os
import sys
import xml.etree.ElementTree as ET
from svgpathtools import parse_path

CANVAS_W = 768.0
CANVAS_H = 1536.0
TARGET_FILL = 0.93


SVG_NS = "{http://www.w3.org/2000/svg}"


def matrix_identity():
    return [1,0,0,1,0,0]


def multiply(a,b):
    return [
        a[0]*b[0]+a[2]*b[1],
        a[1]*b[0]+a[3]*b[1],
        a[0]*b[2]+a[2]*b[3],
        a[1]*b[2]+a[3]*b[3],
        a[0]*b[4]+a[2]*b[5]+a[4],
        a[1]*b[4]+a[3]*b[5]+a[5]
    ]


def parse_transform(text):

    if text is None:
        return matrix_identity()

    text=text.strip()

    if text.startswith("translate"):

        vals=text[text.find("(")+1:text.find(")")].replace(","," ").split()

        tx=float(vals[0])

        ty=float(vals[1]) if len(vals)>1 else 0.0

        return [1,0,0,1,tx,ty]

    if text.startswith("matrix"):

        vals=list(map(float,text[text.find("(")+1:text.find(")")].replace(","," ").split()))

        return vals

    return matrix_identity()


def transform_point(x,y,m):

    return (
        m[0]*x+m[2]*y+m[4],
        m[1]*x+m[3]*y+m[5]
    )


def bounds_of_path(path,matrix):

    xmin=1e20
    ymin=1e20
    xmax=-1e20
    ymax=-1e20

    for seg in path:

        box=seg.bbox()

        xs=[box[0],box[1]]
        ys=[box[2],box[3]]

        for x in xs:
            for y in ys:
                xx,yy=transform_point(x,y,matrix)
                xmin=min(xmin,xx)
                ymin=min(ymin,yy)
                xmax=max(xmax,xx)
                ymax=max(ymax,yy)

    return xmin,ymin,xmax,ymax


def process(svgfile,outfile):

    tree=ET.parse(svgfile)
    root=tree.getroot()

    xmin=1e20
    ymin=1e20
    xmax=-1e20
    ymax=-1e20

    for elem in root.iter():

        if elem.tag!=SVG_NS+"path":
            continue

        d=elem.get("d")
        if not d:
            continue

        p=parse_path(d)

        m=parse_transform(elem.get("transform"))

        bx=bounds_of_path(p,m)

        xmin=min(xmin,bx[0])
        ymin=min(ymin,bx[1])
        xmax=max(xmax,bx[2])
        ymax=max(ymax,bx[3])

    width=xmax-xmin
    height=ymax-ymin

    scale=min(
        CANVAS_W*TARGET_FILL/width,
        CANVAS_H*TARGET_FILL/height
    )

    tx=(CANVAS_W-width*scale)/2-scale*xmin
    ty=(CANVAS_H-height*scale)/2-scale*ymin

    wrapper=ET.Element("g")
    wrapper.set(
        "transform",
        f"translate({tx:.3f},{ty:.3f}) scale({scale:.6f})"
    )

    children=list(root)

    for c in children:
        root.remove(c)
        wrapper.append(c)

    root.append(wrapper)

    root.set("width","768")
    root.set("height","1536")
    root.set("viewBox","0 0 768 1536")

    tree.write(outfile,encoding="utf-8",xml_declaration=True)

    print(os.path.basename(svgfile),"✓")


def main():

    if len(sys.argv)!=3:
        print("Usage:")
        print("python normalize_anatomy.py input_folder output_folder")
        return

    inp=sys.argv[1]
    out=sys.argv[2]

    os.makedirs(out,exist_ok=True)

    for root,dirs,files in os.walk(inp):

        rel=os.path.relpath(root,inp)

        outdir=os.path.join(out,rel)

        os.makedirs(outdir,exist_ok=True)

        for f in files:

            if f.lower().endswith(".svg"):

                process(
                    os.path.join(root,f),
                    os.path.join(outdir,f)
                )

if __name__=="__main__":
    main()
