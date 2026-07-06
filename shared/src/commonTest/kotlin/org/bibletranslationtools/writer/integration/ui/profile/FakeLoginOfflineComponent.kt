package org.bibletranslationtools.writer.integration.ui.profile

import org.bibletranslationtools.writer.ui.profile.LoginOfflineComponent

class FakeLoginOfflineComponent : LoginOfflineComponent {

    var onContinueCalled = false
    var lastFullName: String? = null
    var onCancelCalled = false

    override fun onContinue(fullName: String) {
        onContinueCalled = true
        lastFullName = fullName
    }

    override fun onCancel() {
        onCancelCalled = true
    }
}
