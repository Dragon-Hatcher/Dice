import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.sqrt

class JPicture(val image: BufferedImage): JPanel(), MouseListener {

    init {
        this.preferredSize = Dimension(image.width, image.height)
        this.addMouseListener(this)
    }

    override fun paint(g: Graphics?) {
        super.paint(g)
        g as Graphics2D

        g.drawImage(image, 0, 0, null)
    }

    fun drawCircle(ex: Int, ey: Int, r: Int, color: Color) {
        for (x: Int in (ex - r)..(ex + r)) {
            for (y: Int in (ey - r)..(ey + r)) {
                if (x >= 0 && x < image.width && y >= 0 && y < image.height) {
                    val distance = sqrt(((x - ex) * (x - ex) + (y - ey) * (y - ey)).toDouble())
                    if (distance <= r) {
                        image.setRGB(x, y, color.rgb)
                    }
                }
            }
        }
        repaint()
    }

    override fun mouseClicked(e: MouseEvent) {
        val r = circleHere(image, e.x, e.y, true)
        println("${e.x}, ${e.y}: $r")

        r ?: return
        drawCircle(e.x, e.y, r, Color.RED)
    }

    override fun mousePressed(e: MouseEvent?) {}
    override fun mouseReleased(e: MouseEvent?) {}
    override fun mouseEntered(e: MouseEvent?) {}
    override fun mouseExited(e: MouseEvent?) {}
}