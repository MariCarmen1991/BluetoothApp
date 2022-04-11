package com.example.bluetoothapp.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothapp.R
import com.example.bluetoothapp.data.Device

class DevicesAdapter(private var listDevices: ArrayList<Device> ) : RecyclerView.Adapter<DevicesAdapter.MyViewHolder>(){


    private lateinit var mListener: View.OnClickListener


    interface OnClickListener : View.OnClickListener {
        fun onItemClick(position: Int)
        override fun onClick(p0: View?) {
            TODO("Not yet implemented")
        }
    }

    fun setOnItemClickListener(listener: OnClickListener){

        mListener=listener

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_device_layout,parent,false)


        return MyViewHolder(v, mListener)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = listDevices[position]

        holder.deviceName.text = currentItem.name
        holder.deviceAlias.text = currentItem.alias
        holder.deviceAdress.text= currentItem.adress



    }

    override fun getItemCount(): Int = listDevices.size

    class MyViewHolder(itemView: View, listener: View.OnClickListener): RecyclerView.ViewHolder(itemView){

        var deviceName=itemView.findViewById<TextView>(R.id.nameDevice)
        var deviceAlias=itemView.findViewById<TextView>(R.id.aliasDevice)
        var deviceAdress= itemView.findViewById<TextView>(R.id.adressDevice)


        init {
            itemView.setOnClickListener{
                //listener.onItemClick(bindingAdapterPosition)
            }
        }

    }

}