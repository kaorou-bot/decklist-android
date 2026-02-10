package com.mtgo.decklistmanager.ui.folder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mtgo.decklistmanager.R
import com.mtgo.decklistmanager.databinding.ActivityFoldersBinding
import com.mtgo.decklistmanager.domain.model.Folder
import com.mtgo.decklistmanager.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * FoldersActivity - 文件夹管理页面
 */
@AndroidEntryPoint
class FoldersActivity : BaseActivity() {

    private lateinit var binding: ActivityFoldersBinding
    private lateinit var viewModel: FolderViewModel
    private lateinit var adapter: FolderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFoldersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        loadFolders()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.folders)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[FolderViewModel::class.java]

        lifecycleScope.launch {
            viewModel.folders.collect { folders ->
                adapter.submitList(folders)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is FolderUiState.Loading -> {
                        // Show loading if needed
                    }
                    is FolderUiState.Success -> {
                        adapter.submitList(state.folders)
                    }
                    is FolderUiState.Error -> {
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = FolderAdapter(
            onFolderClick = { /* TODO: Navigate to folder contents */ },
            onFolderLongClick = { folder -> showFolderOptionsDialog(folder) },
            onFolderDelete = { folder -> confirmDeleteFolder(folder) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.emptyView.text = getString(R.string.no_folders)
    }

    private fun loadFolders() {
        viewModel.loadFolders()
    }

    private fun showCreateFolderDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.folderNameInput)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.create_folder)
            .setView(dialogView)
            .setPositiveButton(R.string.create) { dialog, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createFolder(name)
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showFolderOptionsDialog(folder: Folder) {
        // TODO: Show options to rename or change color
    }

    private fun confirmDeleteFolder(folder: Folder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_folder)
            .setMessage(getString(R.string.confirm_delete_folder, folder.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteFolder(folder.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_folders, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_create_folder -> {
                showCreateFolderDialog()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showError(message: String) {
        // Show error toast or snackbar
    }
}
