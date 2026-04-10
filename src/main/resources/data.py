import json
import numpy as np
from astropy.cosmology import Planck18 as cosmo

# Load your JSON file
with open("galaxies.json", "r") as f:
    data = json.load(f)

rows = data[0]["Rows"]

# Extract arrays
ra = np.array([row["ra"] for row in rows])      # degrees
dec = np.array([row["dec"] for row in rows])    # degrees
z = np.array([row["z"] for row in rows])

# Convert to radians
ra_rad = np.radians(ra)
dec_rad = np.radians(dec)

# Convert redshift -> comoving distance (Mpc)
r = cosmo.comoving_distance(z).value

# Convert to Cartesian coordinates
x = r * np.cos(dec_rad) * np.cos(ra_rad)
y = r * np.cos(dec_rad) * np.sin(ra_rad)
z_cart = r * np.sin(dec_rad)

# Save output
output = []
for i in range(len(x)):
    output.append({
        "x": float(x[i]),
        "y": float(y[i]),
        "z": float(z_cart[i])
    })

with open("galaxies_xyz.json", "w") as f:
    json.dump(output, f, indent=2)

print("Done. Saved to galaxies_xyz.json")




input_file = "galaxies_xyz.json"
output_file = "points.txt"

# Load JSON data
with open(input_file, "r") as f:
    data = json.load(f)

# Write to text file
n=0;
with open(output_file, "w") as f:
    for point in data:
        x = point["x"]
        y = point["y"]
        z = point["z"]
        f.write(f"{x} {y} {z}\n")
        n=max(n,abs(x));
        n=max(n,abs(y));
        n=max(n,abs(z));

print("Done with inputting data to points.txt. Maximum abs is",n)
