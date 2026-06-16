import os
import math
from PIL import Image, ImageOps

source_path = r"C:\Users\panthula\.gemini\antigravity-ide\brain\ba5b76c3-627d-480e-831a-25cfa957de0e\media__1781587680076.png"
output_dir = r"c:\Users\panthula\Desktop\Nt\src\main\resources\com\ravi\notesapp\icons"

os.makedirs(output_dir, exist_ok=True)

try:
    img = Image.open(source_path).convert("RGBA")
    
    # 1. Remove background
    pixels = img.load()
    bg_color = pixels[0, 0][:3]  # assume top-left pixel is background
    
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = pixels[x, y]
            
            # calculate distance from background color
            dist = math.sqrt((r - bg_color[0])**2 + (g - bg_color[1])**2 + (b - bg_color[2])**2)
            
            if dist < 30:
                pixels[x, y] = (r, g, b, 0) # fully transparent
            elif dist < 80:
                # soft alpha transition for anti-aliased edges
                alpha = int(((dist - 30) / 50.0) * 255)
                pixels[x, y] = (r, g, b, min(a, alpha))

    # Save a generic app icon (512x512)
    app_icon = img.resize((512, 512), Image.Resampling.LANCZOS)
    app_icon.save(os.path.join(output_dir, "app_icon.png"))
    
    # Also overwrite the app_icon.png in the main folder so the app uses it
    app_icon.save(r"c:\Users\panthula\Desktop\Nt\src\main\resources\com\ravi\notesapp\app_icon.png")

    # Save as ICO (Windows icon format)
    icon_sizes = [(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
    img.save(os.path.join(output_dir, "app_icon.ico"), format="ICO", sizes=icon_sizes)

    # Function to create padded/centered icons for specific sizes
    def create_padded_icon(width, height):
        # Increased size from 0.8 to 0.95
        icon_dim = int(min(width, height) * 0.95)
        resized_icon = img.resize((icon_dim, icon_dim), Image.Resampling.LANCZOS)
        
        # Create transparent background
        new_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        
        # Calculate position to paste
        pos = ((width - icon_dim) // 2, (height - icon_dim) // 2)
        new_img.paste(resized_icon, pos, resized_icon)
        
        return new_img

    # Generate the requested formats
    create_padded_icon(1080, 1080).save(os.path.join(output_dir, "icon_1080x1080.png"))
    create_padded_icon(720, 1080).save(os.path.join(output_dir, "icon_720x1080.png"))
    create_padded_icon(340, 720).save(os.path.join(output_dir, "icon_340x720.png"))

    print("Icons generated successfully with transparent background!")
except Exception as e:
    print(f"Error: {e}")
