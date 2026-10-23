package com.example.noteadminapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.noteadminapp.databinding.ItemNoteBinding

// Adapter nhận vào: danh sách note, hàm xử lý khi bấm Sửa, hàm xử lý khi bấm Xóa
class NoteAdapter(
    private var notes: List<NoteWithId>,
    private val onEdit: (NoteWithId) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val item = notes[position]
        // Gán dữ liệu Text
        holder.binding.itemTitle.text = item.note.title
        holder.binding.itemDesc.text = item.note.description



        // Gán sự kiện click
        holder.binding.btnEdit.setOnClickListener { onEdit(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item.id) }
    }

    override fun getItemCount(): Int = notes.size

    // Hàm helper để cập nhật dữ liệu mới cho adapter
    fun submitList(newNotes: List<NoteWithId>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}