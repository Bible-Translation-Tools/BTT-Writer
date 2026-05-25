package org.bibletranslationtools.writer.rendering

import org.bibletranslationtools.writer.rendering.model.RenderNode

/**
 * Default rendering engine. Delegates to USXRenderer.
 */
class DefaultRenderer : RenderingEngine() {

    private var renderer: USXRenderer? = null

    override fun render(input: String): List<RenderNode> {
        val usxRenderer = USXRenderer()
        this.renderer = usxRenderer
        if (isStopped()) return listOf(RenderNode.Text(input))
        return usxRenderer.render(input)
    }

    override fun onStop() {
        renderer?.stop()
    }
}
