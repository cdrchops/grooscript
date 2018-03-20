/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grooscript.util

import static org.grooscript.util.Util.SEP
import static org.grooscript.util.FileSolver.DEFAULT_PATH

class FileSolverSpec extends GroovyTestCase {

    void 'file exists'() {
        expect:
        fileSolver.exists(FILE_PATH)
    }

    void testFilePathFromClassName() {
        assert fileSolver.filePathFromClassName('Name', null) == 'Name.groovy'
        assert fileSolver.filePathFromClassName('Name', '') == 'Name.groovy'
        assert fileSolver.filePathFromClassName('Name', '/gol') == "/gol${SEP}Name.groovy"
        assert fileSolver.filePathFromClassName('org.Name', '') == "org${SEP}Name.groovy"
        assert fileSolver.filePathFromClassName('org.Name', "src${SEP}main${SEP}groovy") ==
                "src${SEP}main${SEP}groovy${SEP}org${SEP}Name.groovy"
    }

    void testReadFileContent() {
        assert new File(FILE_PATH).text == fileSolver.readFile(FILE_PATH)
    }

    void testSaveAFile() {
        def fileName = 'CASUAL'
        def content = 'content file'

        fileSolver.saveFile(fileName, content)

        assert new File(fileName).text == content

        cleanup:
        new File(fileName).delete()
    }

    void 'test is folder'() {
        expect:
        fileSolver.isFolder('.')
    }

    void 'get first classpath folder from conversion options'() {
        given:
        new File(GOOD_CLASSPATH).mkdir()

        expect:
        fileSolver.classPathFolder(classpath) == expectedClassPath

        cleanup:
        new File(GOOD_CLASSPATH).deleteDir()

        where:
        classpath               | expectedClassPath
        null                    | DEFAULT_PATH
        GOOD_CLASSPATH          | GOOD_CLASSPATH
        'any'                   | DEFAULT_PATH
        ['any', GOOD_CLASSPATH] | GOOD_CLASSPATH
        ['any', 'other']        | DEFAULT_PATH
    }

    private static final FILE_PATH = 'LICENSE.txt'
    private static final GOOD_CLASSPATH = 'good'
    private FileSolver fileSolver = new FileSolver()
}
