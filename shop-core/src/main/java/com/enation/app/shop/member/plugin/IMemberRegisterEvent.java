package com.enation.app.shop.member.plugin;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enation.app.base.core.model.Member;

/**
 * 会员注册事件
 * @author kingapex
 *
 */
public interface IMemberRegisterEvent {
	
	/**
	 * 会员注册时激发此事件
	 * @param member 会员实体
	 */
	@Transactional(propagation = Propagation.REQUIRED)  
	public void onRegister(Member member);
	
	
	
}