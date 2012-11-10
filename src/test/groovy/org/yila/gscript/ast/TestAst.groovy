package org.yila.gscript.ast

import org.yila.gscript.GsConverter
import org.yila.gscript.test.TestJs
import spock.lang.Specification
import org.yila.gscript.util.Util

/**
 * First test for converts groovy code to javascript code
 * Following GroovyInAction Book
 * Chap 2. Groovy basics
 * JFL 27/08/12
 */
class TestAst extends Specification {

    def converter = new GsConverter()

    def readAndConvert(nameOfFile,consoleOutput) {

        def file = TestJs.getGroovyTestScript(nameOfFile)

        def result = Util.fullProcessScript(file.text)

        if (consoleOutput) {
            println 'jsScript->\n'+result.jsScript
        }

        return result
    }


    def 'test GsNotConvert' () {
        when:
        def result = readAndConvert('asts/NotConvert',false)

        then:
        !result.assertFails
        result.jsScript.indexOf('NotConvert')<0

    }

}