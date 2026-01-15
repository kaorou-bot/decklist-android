package com.mtgo.decklistmanager.ui.base

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * BaseActivity - 所有 Activity 的基类
 * 提供通用的功能以减少代码重复
 */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * 通用的进度遮罩层
     */
    protected var progressOverlay: View? = null

    /**
     * 显示加载状态
     */
    protected fun showLoading() {
        progressOverlay?.visibility = View.VISIBLE
    }

    /**
     * 隐藏加载状态
     */
    protected fun hideLoading() {
        progressOverlay?.visibility = View.GONE
    }

    /**
     * 显示确认对话框
     */
    protected fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * 显示信息对话框
     */
    protected fun showInfoDialog(
        title: String,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                onDismiss()
            }
            .setOnCancelListener {
                onDismiss()
            }
            .show()
    }

    /**
     * 收集 Flow 的辅助方法
     */
    protected fun <T> collectFlow(
        flow: Flow<T>,
        collector: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { value ->
                    collector(value)
                }
            }
        }
    }

    /**
     * 显示单选对话框
     */
    protected fun showSingleChoiceDialog(
        title: String,
        items: Array<String>,
        onSelected: (Int, String) -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, -1) { dialog, which ->
                val selected = items[which]
                onSelected(which, selected)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * 显示带输入的对话框
     */
    protected fun showInputDialog(
        title: String,
        message: String? = null,
        hint: String? = null,
        onConfirm: (String) -> Unit
    ) {
        val input = android.widget.EditText(this).apply {
            hint?.let { setText(it) }
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                onConfirm(input.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
