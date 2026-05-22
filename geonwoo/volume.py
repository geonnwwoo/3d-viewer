step = 0.037  # ~28 points per axis → about 21,952 raw points
half = 0.5

points = []

def add_point(x, y, z):
    points.append(f"{x:.3f} {y:.3f} {z:.3f}")

# Fill entire cube volume
n = int(1 / step)

for i in range(n + 1):
    x = -half + i * step

    for j in range(n + 1):
        y = -half + j * step

        for k in range(n + 1):
            z = -half + k * step
            add_point(x, y, z)

# Remove any accidental duplicates
points = list(dict.fromkeys(points))

# Trim to <= 20,000 points if needed
points = points[:20000]

# Write to cube.txt
with open("volume.txt", "w") as f:
    for p in points:
        f.write(p + "\n")

print(f"cube.txt generated with {len(points)} points.")
