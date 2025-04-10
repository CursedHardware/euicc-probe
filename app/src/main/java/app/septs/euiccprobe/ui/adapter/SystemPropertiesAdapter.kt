package app.septs.euiccprobe.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.septs.euiccprobe.ui.widget.ListItemView

class SystemPropertiesAdapter(private val properties: Map<String, String>) :
    RecyclerView.Adapter<SystemPropertiesAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = ListItemView(viewGroup.context)
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val itemView = viewHolder.itemView as ListItemView
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        itemView.layoutParams = layoutParams
        val key = properties.keys.elementAt(position)
        itemView.headlineText = key
        itemView.supportingText = properties[key]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = properties.size

}