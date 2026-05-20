step = 0.001
half = 0.5

points = []

def add_point(x, y, z):
    points.append(f"{x:.2f} {y:.2f} {z:.2f}")

# Generate points along all 12 edges
for i in range(int(1 / step) + 1):
    t = -half + i * step

    # x-direction edges
    add_point(t, -half, -half)
    add_point(t, -half,  half)
    add_point(t,  half, -half)
    add_point(t,  half,  half)

    # y-direction edges
    add_point(-half, t, -half)
    add_point(-half, t,  half)
    add_point( half, t, -half)
    add_point( half, t,  half)

    # z-direction edges
    add_point(-half, -half, t)
    add_point(-half,  half, t)
    add_point( half, -half, t)
    add_point( half,  half, t)

# Remove duplicate corner points
points = list(dict.fromkeys(points))

# Write to cube.txt
with open("cube.txt", "w") as f:
    for p in points:
        f.write(p + "\n")

print("cube.txt generated.")
