package erasom.juanchito.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GameAdapter(
    private val games: List<Game>,
    private var selectedIndex: Int = 0,
    private val onSelected: (Game, Int) -> Unit
) : RecyclerView.Adapter<GameAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvGameName)
        val genre: TextView = view.findViewById(R.id.tvGameGenre)
        val badge: TextView = view.findViewById(R.id.tvCartridgeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val game = games[position]
        val meta = GameMetadataProvider.getMetadata(game.name)
        val density = holder.itemView.context.resources.displayMetrics.density

        holder.name.text = game.name
        holder.genre.text = game.genre.uppercase()
        holder.badge.text = if (position < 15) "NES" else "ARCADE"

        val isSelected = position == selectedIndex
        val strokeColor = if (isSelected) meta.accentColor else meta.primaryColor
        val bgColor = if (isSelected) meta.primaryColor else 0xCC15151F.toInt()

        val bg = GameMetadataProvider.createCartridgeDrawable(
            primary = bgColor,
            accent = strokeColor,
            radiusDp = 8f,
            density = density
        )
        holder.itemView.background = bg

        holder.itemView.setOnClickListener {
            val prev = selectedIndex
            selectedIndex = holder.bindingAdapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedIndex)
            onSelected(game, selectedIndex)
        }
    }

    fun setSelection(index: Int) {
        val prev = selectedIndex
        selectedIndex = index
        notifyItemChanged(prev)
        notifyItemChanged(selectedIndex)
    }

    fun getSelectedGame(): Game = games[selectedIndex]

    override fun getItemCount(): Int = games.size
}
