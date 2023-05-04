package com.example.nearbyapi.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.nearbyapi.model.Endpoint
import com.example.nearbyapi.R

class EndpointListAdapter(private val context: Context, private val endpoints: MutableList<Endpoint>) :
    BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val endpoint = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_endpoint, parent, false)

        view.findViewById<TextView>(R.id.endpointName).text = endpoint.name
        view.findViewById<TextView>(R.id.endpointId).text = endpoint.id

        return view
    }

    override fun getItem(position: Int): Endpoint {
        return endpoints[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return endpoints.size
    }

    fun updateEndpoints(newEndpoints: List<Endpoint>) {
        endpoints.clear()
        endpoints.addAll(newEndpoints)
        notifyDataSetChanged()
    }
}
