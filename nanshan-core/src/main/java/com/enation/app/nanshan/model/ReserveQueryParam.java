package com.enation.app.nanshan.model;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class ReserveQueryParam implements Serializable{

   
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private String articleName;
    private String articleId;
    private String memberName;
    private String isDel;
    private String memberId;
    private String phoneNumber;
    private String email;
    
	
	public String getArticleName() {
		return articleName;
	}
	public void setArticleName(String articleName) {
		this.articleName = articleName;
	}
	public String getArticleId() {
		return articleId;
	}
	public void setArticleId(String articleId) {
		this.articleId = articleId;
	}
	public String getMemberName() {
		return memberName;
	}
	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}
	public String getIsDel() {
		return isDel;
	}
	public void setIsDel(String isDel) {
		this.isDel = isDel;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getMemberId() {
		return memberId;
	}
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
    
	
	
    
	

}
