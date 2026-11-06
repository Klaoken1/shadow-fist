import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

// Renders the launcher icon (same art as res/drawable/ic_fg.xml) to the PNGs iOS and the web app need.
// usage: java tools/GenIcon.java   (from the project root)
public class GenIcon {
    public static void main(String[] a) throws Exception {
        write(180, "web/icon-180.png");
        write(192, "web/icon-192.png");
        write(512, "web/icon-512.png");
        write(1024, "ios/ShadowFist/Assets.xcassets/AppIcon.appiconset/icon-1024.png");
    }

    static void write(int size, String path) throws Exception {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB); // no alpha: the App Store rejects it
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(size / 108.0, size / 108.0);
        g.setColor(new Color(0xEE6A3F)); g.fill(new Rectangle2D.Double(0, 0, 108, 108));
        g.setColor(new Color(0xFFE3BA)); g.fill(new Ellipse2D.Double(36, 28, 44, 44));
        g.setColor(new Color(0x120E14)); g.fill(new Rectangle2D.Double(0, 79, 108, 29));
        g.setColor(new Color(0xE63946)); g.setStroke(round(3.2f));
        Path2D scarf = new Path2D.Double();
        scarf.moveTo(40, 38); scarf.curveTo(35, 36, 32, 41, 28, 38); scarf.curveTo(26, 36.5, 25, 38, 24, 37.5);
        g.draw(scarf);
        g.setColor(new Color(0x0B0A0E)); g.fill(new Ellipse2D.Double(37.5, 31, 12, 12));
        g.setStroke(round(4.2f));
        line(g, 45, 43, 52, 60); line(g, 52, 60, 64.5, 57, 78, 55); line(g, 52, 60, 45, 69, 50, 79);
        line(g, 46.5, 47, 40, 53, 33, 50); line(g, 46.5, 47, 54, 44, 60, 38);
        g.setColor(Color.WHITE); g.setStroke(round(1.2f)); line(g, 45.2, 36.4, 48.2, 35.9);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    static Stroke round(float w) { return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }

    static void line(Graphics2D g, double... p) {
        Path2D l = new Path2D.Double(); l.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) l.lineTo(p[i], p[i + 1]);
        g.draw(l);
    }
}
