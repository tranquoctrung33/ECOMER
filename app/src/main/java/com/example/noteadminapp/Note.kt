package com.example.noteadminapp

/**
 * Lớp đại diện cho dữ liệu của một Note trên Firestore.
 * Firestore yêu cầu phải có constructor rỗng (hoặc các tham số có giá trị mặc định)
 * để có thể tự động chuyển đổi từ Document sang Object.
 */
data class Note(
    var title: String = "",
    var description: String = "",

)

/**
 * Lớp wrapper để kèm theo ID của document.
 * ID này cần thiết khi muốn Cập nhật (Update) hoặc Xóa (Delete) một note cụ thể.
 * Chúng ta tách riêng ra để khi lưu ngược Note lên Firestore, ID không bị ghi vào thành một trường dữ liệu thừa.
 */
data class NoteWithId(
    val id: String,
    val note: Note
)