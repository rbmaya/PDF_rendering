package merybina.example.pdfrendering.ui

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import merybina.example.pdfrendering.databinding.PdfViewLayoutBinding

class PdfRendererAdapter(
    private val renderPage: (pageIndex: Int) -> Bitmap?,
) : RecyclerView.Adapter<PdfRendererAdapter.PageViewHolder>() {

    var pages: Map<Int, Bitmap?> = mapOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = PdfViewLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val bitmap = pages[position] ?: renderPage(position)
        bitmap?.let { holder.bind(it) }
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        pages[holder.bindingAdapterPosition]?.recycle()
    }

    inner class PageViewHolder(private val binding: PdfViewLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bitmap: Bitmap) {
            binding.imageView.setImageBitmap(bitmap)
        }
    }
}