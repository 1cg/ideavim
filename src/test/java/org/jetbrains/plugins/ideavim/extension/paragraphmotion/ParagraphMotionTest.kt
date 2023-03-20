/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.paragraphmotion

import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class ParagraphMotionTest : VimTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("vim-paragraph-motion")
  }

  @Test
  fun `test paragraph next without whitespace`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |$c
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test paragraph next with whitespace`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    val after = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |$c....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    doTest("}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test paragraph next with whitespace visual`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    val after = """Lorem ipsum dolor sit amet,
        |${s}consectetur adipiscing elit
        |$c.$se...
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    doTest("v}", before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @Test
  fun `test paragraph next with whitespace delete`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    val after = """Lorem ipsum dolor sit amet,
        |$c
        |....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    doTest("d}", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test paragraph prev without whitespace`() {
    val before = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |$c
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest("{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test paragraph prev with whitespace`() {
    val before = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |....
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    val after = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |$c....
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    doTest("{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun `test paragraph prev with whitespace visual`() {
    val before = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |....
        |${c}where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin().dotToSpace()
    val after = """I found it in a legendary land
        |all rocks and lavender and tufted grass,
        |${s}$c....
        |w${se}here it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
    """.trimMargin().dotToSpace()
    doTest("v{", before, after, VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
  }

  @Test
  fun `test paragraph prev with whitespace delete`() {
    val before = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |....
        |${c}Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    val after = """Lorem ipsum dolor sit amet,
        |consectetur adipiscing elit
        |$c
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin().dotToSpace()
    doTest("d{", before, after, VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }
}
