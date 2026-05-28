package org.bibletranslationtools.writer.integration.ui.publish

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.bibletranslationtools.writer.core.TargetTranslation
import org.bibletranslationtools.writer.core.Validation
import org.bibletranslationtools.writer.ui.publish.PublishComponent

class FakePublishComponent : PublishComponent {
    override val state = MutableStateFlow(PublishComponent.State())
    override val dialogSlot: Value<ChildSlot<*, PublishComponent.DialogChild>> = MutableValue(ChildSlot<Any, PublishComponent.DialogChild>())

    override val targetTranslation: TargetTranslation = mockk(relaxed = true)

    var openReviewCalledWith: Validation.InvalidFrame? = null
    var refreshContributorsCalled = false
    var showExportDialogCalled = false
    var dismissDialogCalled = false
    var navigateBackCalled = false

    override fun openReview(item: Validation.InvalidFrame) {
        openReviewCalledWith = item
    }

    override fun refreshContributors() {
        refreshContributorsCalled = true
    }

    override fun showExportDialog() {
        showExportDialogCalled = true
    }

    override fun dismissDialog() {
        dismissDialogCalled = true
    }

    override fun navigateBack() {
        navigateBackCalled = true
    }
}
