#!/usr/bin/env python3
def obj_to_txt(input_file, output_vertices_file, output_lines_file, output_mesh_file):
    vertices=[]
    edges=set()
    mesh=[]
    
    print(f"Reading {input_file}...")
    
    try:
        with open(input_file, 'r') as f:
            for line in f:
                line = line.strip()
                
                if not line or line.startswith('#'):
                    continue
                
                if line.startswith('v '):
                    parts = line.split()
                    if len(parts) >= 4: 
                        x = float(parts[1])
                        y = float(parts[2])
                        z = float(parts[3])
                        vertices.append((x, y, z))
                
                elif line.startswith('f '):
                    parts = line.split()
                    indices = []
                    for part in parts[1:]:
                        vertex_idx = int(part.split('/')[0]) - 1
                        indices.append(vertex_idx)
                    
                    for i in range(len(indices)):
                        v1 = indices[i]
                        v2 = indices[(i + 1) % len(indices)] 
                        edge = (min(v1, v2), max(v1, v2))
                        edges.add(edge)
                    
                    if len(indices) >= 3:
                        for i in range(1, len(indices) - 1):
                            mesh.append((indices[0], indices[i], indices[i + 1]))
        
        print(f"Found {len(vertices)} vertices")
        print(f"Found {len(edges)} unique edges")
        print(f"Found {len(mesh)} triangles")
        
        print(f"Writing vertices to {output_vertices_file}...")
        with open(output_vertices_file, 'w') as f:
            for x, y, z in vertices:
                f.write(f"{x:.2f} {y:.2f} {z:.2f}\n")
        print(f"Done! Wrote {len(vertices)} vertices")
        
        print(f"Writing edges to {output_lines_file}...")
        with open(output_lines_file, 'w') as f:
            for v1, v2 in sorted(edges):
                f.write(f"{v1} {v2}\n")
        print(f"Done! Wrote {len(edges)} edges")
        
        print(f"Writing mesh to {output_mesh_file}...")
        with open(output_mesh_file, 'w') as f:
            for p1, p2, p3 in mesh:
                f.write(f"{p1} {p2} {p3}\n")
        print(f"Done! Wrote {len(mesh)} triangles")
        
        print("\nFirst 5 mesh triangles:")
        for i, (p1, p2, p3) in enumerate(mesh[:5]):
            print(f"{p1} {p2} {p3}")
        
    except FileNotFoundError:
        print(f"Error: Could not find file '{input_file}'")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    # Usage
    input_obj = "lucy.obj"
    output_vertices = "lucy.txt"
    output_lines = "lucy_lines.txt"
    output_mesh = "lucy_mesh.txt"
    
    obj_to_txt(input_obj, output_vertices, output_lines, output_mesh)
