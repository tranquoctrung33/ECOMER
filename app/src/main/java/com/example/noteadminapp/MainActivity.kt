package com.example.noteadminapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.noteadminapp.databinding.ActivityMainBinding
import com.example.noteadminapp.databinding.DialogLoadingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NoteAdapter
    private lateinit var loadingDialog: AlertDialog

    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()
    private val notesCollection = firestore.collection("notes")
    private var snapshotListener: ListenerRegistration? = null

    // Biến trạng thái để biết đang Thêm mới hay Sửa
    private var currentEditingNoteId: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kiểm tra đăng nhập lần nữa cho chắc
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoadingDialog()
        setupRecyclerView()
        setupEventListeners()
        startRealtimeUpdates() // Bắt đầu lắng nghe dữ liệu
    }

    private fun setupLoadingDialog() {
        // Tạo dialog xoay xoay để chặn người dùng thao tác khi đang upload
        val dialogBinding = DialogLoadingBinding.inflate(layoutInflater)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        // Lưu ý: Bạn cần tạo thêm layout dialog_loading.xml đơn giản chứa ProgressBar
        // Nếu lười, có thể dùng ProgressDialog (deprecated nhưng vẫn chạy) hoặc bỏ qua bước này.
    }

    private fun setupRecyclerView() {
        adapter = NoteAdapter(emptyList(),
            onEdit = { noteWithId -> showEditForm(noteWithId) },
            onDelete = { id -> deleteNote(id) }
        )
        binding.recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotes.adapter = adapter
    }

    private fun setupEventListeners() {
        // Nút Đăng xuất
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }


        // Nút Submit (Thêm hoặc Cập nhật)
        binding.btnSubmit.setOnClickListener {
            handleSubmit()
        }

        // Nút Hủy sửa
        binding.btnClear.setOnClickListener {
            resetForm()
        }
    }

    // --- PHẦN 1: READ (Đọc dữ liệu Realtime) ---
    private fun startRealtimeUpdates() {
        // Lắng nghe thay đổi từ collection "notes", sắp xếp theo title (hoặc trường khác tùy bạn)
        snapshotListener = notesCollection.orderBy("title", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val notesList = snapshots.map { doc ->
                        NoteWithId(doc.id, doc.toObject(Note::class.java))
                    }
                    adapter.submitList(notesList)
                }
            }
    }

    // --- PHẦN 2: CREATE & UPDATE (Thêm/Sửa) ---
    private fun handleSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Nhập tiêu đề!"
            return
        }

        // Lưu trực tiếp, không cần quan tâm đến ảnh nữa
        saveNoteToFirestore(Note(title, desc))
    }



    private fun saveNoteToFirestore(note: Note) {
        if (currentEditingNoteId == null) {
            // --> Đang THÊM MỚI
            notesCollection.add(note)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
                .addOnFailureListener { e -> Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show() }
            // .addOnCompleteListener { loadingDialog.dismiss() }
        } else {
            // --> Đang CẬP NHẬT existing document
            notesCollection.document(currentEditingNoteId!!).set(note)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật xong!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
                .addOnFailureListener { e -> Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show() }
            // .addOnCompleteListener { loadingDialog.dismiss() }
        }
    }

    // --- PHẦN 3: DELETE (Xóa) ---
    private fun deleteNote(docId: String) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa không?")
            .setPositiveButton("Xóa ngay") { _, _ ->
                notesCollection.document(docId).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(this, "Lỗi xóa: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Không", null)
            .show()
    }

    // --- CÁC HÀM PHỤ TRỢ UI ---
    private fun showEditForm(item: NoteWithId) {
        // Đưa dữ liệu lên form
        currentEditingNoteId = item.id

        binding.etTitle.setText(item.note.title)
        binding.etDescription.setText(item.note.description)



        // Đổi trạng thái UI
        binding.btnSubmit.text = "Lưu cập nhật"
        binding.btnClear.visibility = View.VISIBLE

        // Cuộn lên đầu để user thấy form
        binding.formScrollView.smoothScrollTo(0, 0)
    }

    private fun resetForm() {
        currentEditingNoteId = null


        binding.etTitle.text.clear()
        binding.etDescription.text.clear()


        binding.btnSubmit.text = "Thêm mới"
        binding.btnClear.visibility = View.GONE
        binding.etTitle.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        snapshotListener?.remove() // Dừng lắng nghe khi thoát app để tránh tốn tài nguyên
    }
}