/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/idea-completion/testData/kdoc")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class KDocCompletionTestGenerated extends AbstractJvmBasicCompletionTest {
    public void testAllFilesPresentInKdoc() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/idea-completion/testData/kdoc"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("FQLink.kt")
    public void testFQLink() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/FQLink.kt");
        doTest(fileName);
    }

    @TestMetadata("Link.kt")
    public void testLink() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/Link.kt");
        doTest(fileName);
    }

    @TestMetadata("MemberEnumEntryFQLink.kt")
    public void testMemberEnumEntryFQLink() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/MemberEnumEntryFQLink.kt");
        doTest(fileName);
    }

    @TestMetadata("MemberEnumEntryLink.kt")
    public void testMemberEnumEntryLink() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/MemberEnumEntryLink.kt");
        doTest(fileName);
    }

    @TestMetadata("MemberLink.kt")
    public void testMemberLink() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/MemberLink.kt");
        doTest(fileName);
    }

    @TestMetadata("NotTagName.kt")
    public void testNotTagName() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/NotTagName.kt");
        doTest(fileName);
    }

    @TestMetadata("ParamTag.kt")
    public void testParamTag() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/ParamTag.kt");
        doTest(fileName);
    }

    @TestMetadata("SkipExistingParamTag.kt")
    public void testSkipExistingParamTag() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/SkipExistingParamTag.kt");
        doTest(fileName);
    }

    @TestMetadata("TagName.kt")
    public void testTagName() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagName.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameAfterAt.kt")
    public void testTagNameAfterAt() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagNameAfterAt.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameInClass.kt")
    public void testTagNameInClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagNameInClass.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameInExtensionFunction.kt")
    public void testTagNameInExtensionFunction() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagNameInExtensionFunction.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameMiddle.kt")
    public void testTagNameMiddle() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagNameMiddle.kt");
        doTest(fileName);
    }

    @TestMetadata("TagNameStart.kt")
    public void testTagNameStart() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/kdoc/TagNameStart.kt");
        doTest(fileName);
    }
}
