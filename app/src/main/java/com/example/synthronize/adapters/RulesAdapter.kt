package com.example.synthronize.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.synthronize.databinding.ItemRuleBinding
import com.example.synthronize.interfaces.OnCommunityRuleModified
import com.example.synthronize.utils.AppUtil

class RulesAdapter(private val context: Context, private val rules: HashMap<String, String>,
                          private val listener: OnCommunityRuleModified, private val toEdit:Boolean = false):
    RecyclerView.Adapter<RulesAdapter.RuleViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RulesAdapter.RuleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemRuleBinding.inflate(inflater, parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RulesAdapter.RuleViewHolder, position: Int) {
        val keys = rules.keys.toList().sorted()
        holder.bind(keys[position])
    }

    override fun getItemCount(): Int {
        return rules.size
    }


    inner class RuleViewHolder(private val ruleBinding: ItemRuleBinding): RecyclerView.ViewHolder(ruleBinding.root){

        private var rule = ""

        fun bind(key:String){
            rule = rules[key]!!

            refreshRule()

            if (toEdit){
                //buttons
                ruleBinding.saveRuleBtn.setOnClickListener {
                    val newRule = ruleBinding.ruleEdtTxt.text.toString()
                    if (newRule.isEmpty()){
                        Toast.makeText(context, "Please type your rule", Toast.LENGTH_SHORT).show()
                    } else if (AppUtil().containsBadWord(newRule)) {
                        Toast.makeText(context, "Your rule contains sensitive words", Toast.LENGTH_SHORT).show()
                    } else {
                        listener.modifiedRule(key, newRule)
                        refreshRule()
                    }
                }

                ruleBinding.editRuleBtn.setOnClickListener {
                    //hide
                    ruleBinding.ruleTV.visibility = View.GONE
                    ruleBinding.editRuleBtn.visibility = View.GONE
                    //display
                    ruleBinding.ruleEdtTxt.visibility = View.VISIBLE
                    ruleBinding.saveRuleBtn.visibility = View.VISIBLE
                }

                ruleBinding.deleteRuleBtn.setOnClickListener {
                    listener.deleteRule(key)
                }
            } else {
                ruleBinding.deleteRuleBtn.visibility = View.GONE
                ruleBinding.editRuleBtn.visibility = View.GONE
            }
        }

        private fun refreshRule(){
            if (rule.isNotEmpty()){
                //bind rule
                ruleBinding.ruleTV.text = "${position + 1}.) $rule"
                ruleBinding.ruleEdtTxt.setText(rule)

                //hide
                ruleBinding.ruleEdtTxt.visibility = View.GONE
                ruleBinding.saveRuleBtn.visibility = View.GONE
                //display
                ruleBinding.ruleTV.visibility = View.VISIBLE
                ruleBinding.editRuleBtn.visibility = View.VISIBLE
            } else {
                //hide
                ruleBinding.ruleTV.visibility = View.GONE
                ruleBinding.editRuleBtn.visibility = View.GONE
                //display
                ruleBinding.ruleEdtTxt.visibility = View.VISIBLE
                ruleBinding.saveRuleBtn.visibility = View.VISIBLE
            }
        }
    }
}