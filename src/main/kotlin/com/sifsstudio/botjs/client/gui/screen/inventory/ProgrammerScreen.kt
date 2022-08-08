package com.sifsstudio.botjs.client.gui.screen.inventory

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.network.ServerboundScriptChangedPacket
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.client.StringSplitter
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiComponent
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.font.TextFieldHelper
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*
import kotlin.math.max
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
class ProgrammerScreen(private val entityId: Int, private var script: String) :
    Screen(TranslatableComponent("${BotJS.ID}.menu.programmer.title")) {

    companion object {
        private const val PROGRAMMER_WIDTH: Int = 176
        private const val PROGRAMMER_HEIGHT: Int = 165
        private val GUI_TEXTURE_RESOURCE: ResourceLocation =
            ResourceLocation(BotJS.ID, "textures/gui/program_window.png")

        fun findLineFromPos(pLineStarts: IntArray, pFind: Int): Int {
            val i = Arrays.binarySearch(pLineStarts, pFind)
            return if (i < 0) -(i + 2) else i
        }

        @OnlyIn(Dist.CLIENT)
        class DisplayCache(
            private val fullText: String,
            private val lineStarts: IntArray,
            val cursorPos: Pos2i,
            val cursorAtEnd: Boolean,
            val lines: Array<String>,
            val selection: Array<Rect2i>
        ) {

            companion object {
                val EMPTY =
                    DisplayCache("", IntArray(0), Pos2i(0, 0), true, Array(1) { "" }, Array(0) { Rect2i(0, 0, 0, 0) })
            }

            fun getIndexAtPosition(pFont: Font, pCursorPosition: Pos2i): Int {
                val i = pCursorPosition.y / 9
                return if (i < 0) {
                    0
                } else if (i >= lines.size) {
                    fullText.length
                } else {
                    val content = lines[i]
                    lineStarts[i] + pFont.splitter.plainIndexAtWidth(
                        content,
                        pCursorPosition.x, Style.EMPTY
                    )
                }
            }

            fun changeLine(pXChange: Int, pYChange: Int): Int {
                val i = findLineFromPos(lineStarts, pXChange)
                val j = i + pYChange
                val k: Int = if (0 <= j && j < lineStarts.size) {
                    val l = pXChange - lineStarts[i]
                    val i1: Int = lines[j].length
                    lineStarts[j] + l.coerceAtMost(i1)
                } else {
                    pXChange
                }
                return k
            }
        }

        @OnlyIn(Dist.CLIENT)
        data class Pos2i(val x: Int, val y: Int)
    }

    private lateinit var doneButton: Button
    private var frameTick: UInt = 0u
    private val codeEdit = TextFieldHelper({ script },
        { script = it; doneButton.active = true },
        this::getClipboard,
        this::setClipboard,
        { true })
    private var displayCache: DisplayCache? = DisplayCache.EMPTY
        get() {
            if (field == null) {
                field = rebuildDisplayCache()
            }
            return field
        }
    private var lastClickTime: Long = Util.getMillis()
    private var lastIndex: Int = -1

    private fun getClipboard(): String =
        if (minecraft != null) TextFieldHelper.getClipboardContents(getMinecraft()) else ""

    private fun setClipboard(clipboard: String): Unit = if (minecraft != null) {
        TextFieldHelper.setClipboardContents(getMinecraft(), clipboard)
    } else Unit

    override fun init() {
        clearDisplayCache()
        val i = (width - PROGRAMMER_WIDTH) / 2
        val j = (height - PROGRAMMER_HEIGHT) / 2
        doneButton = addRenderableWidget(Button(i + 4, j + 141, 79, 20, CommonComponents.GUI_DONE) {
            NetworkManager.INSTANCE.sendToServer(
                ServerboundScriptChangedPacket(
                    entityId, script
                )
            )
            this.minecraft!!.setScreen(null)
        })
        doneButton.active = false
        addRenderableWidget(Button(i + 93, j + 141, 79, 20, CommonComponents.GUI_CANCEL) {
            this.minecraft!!.setScreen(null)
        })
        getMinecraft().keyboardHandler.setSendRepeatsToGui(true)
    }

    override fun removed() {
        getMinecraft().keyboardHandler.setSendRepeatsToGui(false)
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean =
        if (this.codeKeyPressed(pKeyCode)) {
            clearDisplayCache()
            true
        } else {
            super.keyPressed(pKeyCode, pScanCode, pModifiers)
        }

    override fun charTyped(pCodePoint: Char, pModifiers: Int): Boolean =
        if (super.charTyped(pCodePoint, pModifiers)) {
            true
        } else if (SharedConstants.isAllowedChatCharacter(pCodePoint)) {
            codeEdit.insertText(pCodePoint.toString())
            clearDisplayCache()
            true
        } else {
            false
        }

    private fun codeKeyPressed(pKeyCode: Int): Boolean =
        if (isSelectAll(pKeyCode)) {
            codeEdit.selectAll()
            true
        } else if (isCopy(pKeyCode)) {
            codeEdit.copy()
            true
        } else if (isPaste(pKeyCode)) {
            codeEdit.paste()
            true
        } else if (isCut(pKeyCode)) {
            codeEdit.cut()
            true
        } else {
            when (pKeyCode) {
                257, 335 -> {
                    codeEdit.insertText("\n")
                    true
                }

                258 -> {
                    codeEdit.insertText("    ")
                    true
                }

                259 -> {
                    codeEdit.removeCharsFromCursor(-1)
                    true
                }

                261 -> {
                    codeEdit.removeCharsFromCursor(1)
                    true
                }

                262 -> {
                    codeEdit.moveByChars(1, hasShiftDown())
                    true
                }

                263 -> {
                    codeEdit.moveByChars(-1, hasShiftDown())
                    true
                }

                264 -> {
                    keyDown()
                    true
                }

                265 -> {
                    keyUp()
                    true
                }

                else -> false
            }
        }

    private fun changeLine(pYChange: Int) {
        val i: Int = codeEdit.cursorPos
        val j: Int = displayCache!!.changeLine(i, pYChange)
        codeEdit.setCursorPos(j, hasShiftDown())
    }

    private fun createSelection(pCorner1: Pos2i, pCorner2: Pos2i): Rect2i {
        val screenCorner1 = convertLocalToScreen(pCorner1)
        val screenCorner2 = convertLocalToScreen(pCorner2)
        val i = min(screenCorner1.x, screenCorner2.x)
        val j = max(screenCorner1.x, screenCorner2.x)
        val k = min(screenCorner1.y, screenCorner2.y)
        val l = max(screenCorner1.y, screenCorner2.y)
        return Rect2i(i, k, j - i, l - k)
    }

    private fun createPartialSelection(
        text: String,
        splitter: StringSplitter,
        startPos: Int,
        endPos: Int,
        lineStartY: Int,
        lineStart: Int
    ): Rect2i {
        val s1 = text.substring(lineStart, startPos)
        val s2 = text.substring(lineStart, endPos)
        return createSelection(
            Pos2i(splitter.stringWidth(s1).toInt(), lineStartY),
            Pos2i(splitter.stringWidth(s2).toInt(), lineStartY + font.lineHeight)
        )
    }

    private fun convertLocalToScreen(pLocalPos: Pos2i) =
        Pos2i(pLocalPos.x + (width - PROGRAMMER_WIDTH) / 2 + 4, pLocalPos.y + (height - PROGRAMMER_HEIGHT) / 2 + 5)

    private fun convertScreenToLocal(pScreenPos: Pos2i) =
        Pos2i(pScreenPos.x - (width - PROGRAMMER_WIDTH) / 2 - 4, pScreenPos.y - (height - PROGRAMMER_HEIGHT) / 2 - 5)

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean =
        if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            true
        } else {
            if (pButton == 0) {
                val i = Util.getMillis()
                val j = displayCache!!.getIndexAtPosition(
                    font,
                    convertScreenToLocal(Pos2i(pMouseX.toInt(), pMouseY.toInt()))
                )
                if (j >= 0) {
                    if (j == lastIndex && i - lastClickTime < 250L) {
                        if (!codeEdit.isSelecting) {
                            selectWord(j)
                        } else {
                            codeEdit.selectAll()
                        }
                    } else {
                        codeEdit.setCursorPos(j, hasShiftDown())
                    }
                    clearDisplayCache()
                }
                lastIndex = j
                lastClickTime = i
            }
            true
        }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean =
        if (super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
            true
        } else {
            if (pButton == 0) {
                val i = displayCache!!.getIndexAtPosition(font, convertScreenToLocal(Pos2i(pMouseX.toInt(), pMouseY.toInt())))
                codeEdit.setCursorPos(i, true)
                clearDisplayCache()
            }
            true
        }


    private fun selectWord(pIndex: Int) {
        // TODO: better word detection
        codeEdit.setSelectionRange(
            StringSplitter.getWordPosition(script, -1, pIndex, false),
            StringSplitter.getWordPosition(script, 1, pIndex, false)
        )
    }

    private fun rebuildDisplayCache(): DisplayCache =
        if (script.isEmpty()) {
            DisplayCache.EMPTY
        } else {
            val i = codeEdit.cursorPos
            val j = codeEdit.selectionPos
            val tempLines = script.split("\n")
            val tempLineStarts: MutableList<Int> = mutableListOf(0)
            script.forEachIndexed { index, c -> if (c == '\n') tempLineStarts.add(index + 1) }
            val tempLineStartsArray = tempLineStarts.toIntArray()
            val cursorPos =
                if (i == script.length && tempLines[tempLines.size - 1].isEmpty()) {
                    Pos2i(0, (tempLines.size - 1) * font.lineHeight)
                } else {
                    val k = findLineFromPos(tempLineStartsArray, i)
                    val l = font.width(script.substring(tempLineStarts[k], i))
                    Pos2i(l, k * font.lineHeight)
                }
            val selections: MutableList<Rect2i> = mutableListOf()

            if (i != j) {
                val start = min(i, j)
                val end = max(i, j)
                val startLine = findLineFromPos(tempLineStartsArray, start)
                val endLine = findLineFromPos(tempLineStartsArray, end)
                if (startLine == endLine) {
                    val pixelLineY = startLine * font.lineHeight
                    val lineStartPos = tempLineStartsArray[startLine]
                    selections.add(createPartialSelection(script, font.splitter, start, end, pixelLineY, lineStartPos))
                } else {
                    val nextStartLine =
                        if (startLine + 1 > script.length) script.length else tempLineStartsArray[startLine + 1]
                    selections.add(
                        createPartialSelection(
                            script,
                            font.splitter,
                            start,
                            nextStartLine,
                            startLine * font.lineHeight,
                            tempLineStartsArray[startLine]
                        )
                    )
                    for (intermediateLine in (startLine + 1) until endLine) {
                        val deltaLineY = intermediateLine * font.lineHeight
                        val lineText = script.substring(
                            tempLineStartsArray[intermediateLine],
                            tempLineStartsArray[intermediateLine + 1]
                        )
                        val lineWidth = font.splitter.stringWidth(lineText).toInt()
                        selections.add(
                            createSelection(
                                Pos2i(0, deltaLineY),
                                Pos2i(lineWidth, deltaLineY + font.lineHeight)
                            )
                        )
                    }
                    selections.add(
                        createPartialSelection(
                            script,
                            font.splitter,
                            tempLineStartsArray[endLine],
                            end,
                            endLine * font.lineHeight,
                            tempLineStartsArray[endLine]
                        )
                    )

                }
            }

            DisplayCache(
                script,
                tempLineStartsArray,
                cursorPos,
                i == script.length,
                tempLines.toTypedArray(),
                selections.toTypedArray()
            )
        }

    private fun keyDown() = changeLine(1)

    private fun keyUp() = changeLine(-1)

    private fun clearDisplayCache() {
        displayCache = null
    }

    override fun tick() {
        super.tick()
        ++frameTick
    }

    override fun render(pPoseStack: PoseStack, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        renderBackground(pPoseStack)
        this.focused = null
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F)
        RenderSystem.setShaderTexture(0, GUI_TEXTURE_RESOURCE)
        val i = (width - PROGRAMMER_WIDTH) / 2
        val j = (height - PROGRAMMER_HEIGHT) / 2
        blit(pPoseStack, i, j, 0, 0, PROGRAMMER_WIDTH, PROGRAMMER_HEIGHT)
        displayCache!!.lines.forEachIndexed { index, str ->
            font.draw(
                pPoseStack,
                str,
                (i + 4).toFloat(),
                (j + 5 + index * font.lineHeight).toFloat(),
                0xFFFFFFFF.toInt()
            )
        }
        renderHighlight(displayCache!!.selection)
        renderCursor(pPoseStack, displayCache!!.cursorPos, displayCache!!.cursorAtEnd)
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick)
    }

    private fun renderHighlight(pSelection: Array<Rect2i>) {
        val tesselator = Tesselator.getInstance()
        val bufferBuilder = tesselator.builder
        RenderSystem.setShader(GameRenderer::getPositionShader)
        RenderSystem.setShaderColor(255.0F, 255.0F, 255.0F, 255.0F)
        RenderSystem.disableTexture()
        RenderSystem.enableColorLogicOp()
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE)
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        for (rect2i in pSelection) {
            val i = rect2i.x
            val j = rect2i.y
            val k = i + rect2i.width
            val l = j + rect2i.height
            bufferBuilder.vertex(i.toDouble(), l.toDouble(), 0.0).endVertex()
            bufferBuilder.vertex(k.toDouble(), l.toDouble(), 0.0).endVertex()
            bufferBuilder.vertex(k.toDouble(), j.toDouble(), 0.0).endVertex()
            bufferBuilder.vertex(i.toDouble(), j.toDouble(), 0.0).endVertex()
        }
        tesselator.end()
        RenderSystem.disableColorLogicOp()
        RenderSystem.enableTexture()
    }

    private fun renderCursor(pPoseStack: PoseStack, pCursorPos: Pos2i, pIsEndOfText: Boolean) {
        if (frameTick / 6u % 2u == 0u) {
            val screenCursorPos = convertLocalToScreen(pCursorPos)
            if (!pIsEndOfText) {
                GuiComponent.fill(
                    pPoseStack,
                    screenCursorPos.x,
                    screenCursorPos.y - 1,
                    screenCursorPos.x + 1,
                    screenCursorPos.y + font.lineHeight,
                    0xFFFFFFFF.toInt()
                )
            } else {
                font.draw(pPoseStack, "_", screenCursorPos.x.toFloat(), screenCursorPos.y.toFloat(), 0xFFFFFFFF.toInt())
            }
        }
    }

}