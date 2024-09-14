package com.example.synthronize.interfaces

interface OnCommunityRuleModified {
    fun deleteRule(key:String)
    fun modifiedRule(key: String, rule:String)
}