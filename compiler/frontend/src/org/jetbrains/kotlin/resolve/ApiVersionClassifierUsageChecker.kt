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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors

object ApiVersionClassifierUsageChecker : ClassifierUsageChecker {
    override fun check(
            targetDescriptor: ClassifierDescriptor,
            trace: BindingTrace,
            element: PsiElement,
            languageVersionSettings: LanguageVersionSettings
    ) {
        val access = languageVersionSettings.getAccessToDescriptor(targetDescriptor)
        if (access is Access.Disallowed) {
            trace.report(Errors.API_NOT_AVAILABLE.on(
                    element, access.sinceVersion.versionString, languageVersionSettings.apiVersion.versionString
            ))
        }
    }

    private sealed class Access {
        object Allowed : Access()
        class Disallowed(val sinceVersion: ApiVersion) : Access()
    }

    // TODO (!): hide such descriptors from scopes instead
    private fun LanguageVersionSettings.getAccessToDescriptor(descriptor: DeclarationDescriptor): Access {
        // If there's no @Since annotation or its value is not recognized, allow the access
        val version = descriptor.getSinceKotlinVersion() ?: return Access.Allowed

        // Otherwise allow the access iff the version in @Since is not greater than our API version
        return if (apiVersion < version) Access.Disallowed(version) else Access.Allowed
    }
}
