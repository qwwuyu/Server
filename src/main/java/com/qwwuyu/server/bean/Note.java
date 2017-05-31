package com.qwwuyu.server.bean;

public class Note {
	private Integer id = -1;

	private Integer userId = -1;

	private String nick;

	private String title;

	private String content;

	private Long time;

	public Integer getId() {
		return id;
	}

	public Note setId(Integer id) {
		this.id = id;
		return this;
	}

	public Integer getUserId() {
		return userId;
	}

	public Note setUserId(Integer userId) {
		this.userId = userId;
		return this;
	}

	public String getNick() {
		return nick;
	}

	public Note setNick(String nick) {
		this.nick = nick == null ? null : nick.trim();
		return this;
	}

	public String getTitle() {
		return title;
	}

	public Note setTitle(String title) {
		this.title = title == null ? null : title.trim();
		return this;
	}

	public String getContent() {
		return content;
	}

	public Note setContent(String content) {
		this.content = content == null ? null : content.trim();
		return this;
	}

	public Long getTime() {
		return time;
	}

	public Note setTime(Long time) {
		this.time = time;
		return this;
	}
}