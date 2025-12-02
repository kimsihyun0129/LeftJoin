package com.example.litejoin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.litejoin.databinding.ItemPostBinding
import com.example.litejoin.model.Post
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(private val itemClickListener: (Post) -> Unit) :
    ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.tvPostTitle.text = post.title
            binding.tvShortDescription.text = post.shortDescription

            // 작성일 포맷팅
            post.createdAt?.let {
                val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                binding.tvCreatedAt.text = "작성일: ${dateFormat.format(it)}"
            } ?: run {
                binding.tvCreatedAt.text = "작성일 정보 없음"
            }

            // 아이템 클릭 리스너 설정
            binding.root.setOnClickListener {
                itemClickListener(post)
            }
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.postId == newItem.postId
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}