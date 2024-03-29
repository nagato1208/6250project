package com.me.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.captcha.botdetect.web.servlet.Captcha;
import com.me.dao.CartDAO;
import com.me.dao.CustomerDAO;
import com.me.dao.RestaurantDAO;
import com.me.dao.UserDAO;
import com.me.pojo.Cart;
import com.me.pojo.Customer;
import com.me.pojo.Menu;
import com.me.pojo.Order;
import com.me.pojo.Restaurant;
import com.me.pojo.User;

/**
 * Handles requests for the application home page.
 */
@Controller
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	@RequestMapping(value = "/user/injectionError.htm", method = RequestMethod.GET)
	public String handleError() {

		return "injectionError";
	}
	
	
	@RequestMapping(value = "/injectionError.htm", method = RequestMethod.GET)
	public String handleUError() {

		return "injectionError";
	}
	

	@RequestMapping(value = "/user/login.htm", method = RequestMethod.GET)
	public String showLoginForm() {

		return "user-login";
	}

	@RequestMapping(value = "/user/login.htm", method = RequestMethod.POST)
	public String handleLoginForm(HttpServletRequest request, UserDAO userDao, ModelMap map) {

		HttpSession session = request.getSession();
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		try {
			User u = userDao.get(username, password);

			if (u != null && u.getStatus() == 1 && u.getRole().equals("Customer")) {
				session.setAttribute("user", u);
				map.addAttribute("user", u);
				return "CustomerWorkArea";
			}
			if (u != null && u.getStatus() == 1 && u.getRole().equals("Restaurant")) {
				session.setAttribute("user", u);
				map.addAttribute("user", u);
				return "RestaurantWorkArea";
			}
			else if (u != null && u.getStatus() == 0) {
				map.addAttribute("errorMessage", "Please activate your account to login!");
				boolean resendLink = true;
				map.addAttribute("resendLink", resendLink);
				return "error";
			} 
			else {
				map.addAttribute("errorMessage", "Invalid username/password!");
				return "error";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	@RequestMapping(value = "/user/create.htm", method = RequestMethod.GET)
	public String showCreateForm() {

		return "user-create-form";
	}

	@RequestMapping(value = "/user/create.htm", method = RequestMethod.POST)
	public String handleCreateForm(HttpServletRequest request, UserDAO userDao, RestaurantDAO restaurantDao, CustomerDAO customerDao, CartDAO cartDao, ModelMap map) {
		Captcha captcha = Captcha.load(request, "CaptchaObject");
		String captchaCode = request.getParameter("captchaCode");
		HttpSession session = request.getSession();
		if (captcha.validate(captchaCode)) {
			String useremail = request.getParameter("username");
			String password = request.getParameter("password");
			String role = request.getParameter("role");
			String name = request.getParameter("name");
			String phoneNumber = request.getParameter("phoneNumber");
			String address = request.getParameter("address");
			User user = new User();
			user.setUserEmail(useremail);
			user.setPassword(password);
			user.setRole(role);
			user.setName(name);
			user.setPhoneNumber(phoneNumber);
			user.setAddress(address);
			user.setStatus(0);

			try {
				//User u = userDao.register(user);
				try {
					List<User> userList = userDao.getList();
					for(User u : userList) {
						if(user.getUserEmail().equals(u.getUserEmail())) {
							map.addAttribute("errorMessage", "User email already registered!");
							return "user-create-form";
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(user.getRole().equals("Restaurant")) {
					Restaurant r = new Restaurant();
					r.setAddress(address);
					r.setName(name);
					r.setPassword(password);
					r.setPhoneNumber(phoneNumber);
					r.setRole(role);
					r.setStatus(0);
					r.setUserEmail(useremail);
					r.setMenuSet(new HashSet<Menu>());
					r.setOrderSet(new HashSet<Order>());				
					
					
					Restaurant res = restaurantDao.register(r);
				}
				
				if(user.getRole().equals("Customer")) {
					Customer r = new Customer();
					r.setAddress(address);
					r.setName(name);
					r.setPassword(password);
					r.setPhoneNumber(phoneNumber);
					r.setRole(role);
					r.setStatus(0);
					r.setUserEmail(useremail);
					r.setOrderSet(new HashSet<Order>());
					
					
					Cart c = new Cart();
					r.setCart(c);
					c.setCustomer(r);
					
//					cartDao.createCart(c);
//					r.setCart(c);
										
					Customer res = customerDao.register(r);
				}
				
				
				Random rand = new Random();
				int randomNum1 = rand.nextInt(5000000);
				int randomNum2 = rand.nextInt(5000000);
				try {
					String str = "http://localhost:8080/finalproject/user/validateemail.htm?email=" + useremail + "&key1="
							+ randomNum1 + "&key2=" + randomNum2;
					session.setAttribute("key1", randomNum1);
					session.setAttribute("key2", randomNum2);
					sendEmail(useremail,
							"Click on this link to activate your account : "+ str);
				} catch (Exception e) {
					System.out.println("Email cannot be sent");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			map.addAttribute("errorMessage", "Invalid Captcha!");
			return "user-create-form";
		}

		return "user-created";
	}

	@RequestMapping(value = "/user/forgotpassword.htm", method = RequestMethod.GET)
	public String getForgotPasswordForm(HttpServletRequest request) {
		
		return "forgot-password";
	}

	@RequestMapping(value = "/user/forgotpassword.htm", method = RequestMethod.POST)
	public String handleForgotPasswordForm(HttpServletRequest request, UserDAO userDao) {

		String useremail = request.getParameter("useremail");
		Captcha captcha = Captcha.load(request, "CaptchaObject");
		String captchaCode = request.getParameter("captchaCode");

		if (captcha.validate(captchaCode)) {
			User user = userDao.get(useremail);
			if(user != null) {
				sendEmail(useremail, "Your password is : " + user.getPassword());
				return "forgot-password-success";
			}
			else {
				request.setAttribute("captchamsg", "User Account does not exist");
				return "forgot-password";
			}
			
		} else {
			request.setAttribute("captchamsg", "Captcha not valid");
			return "forgot-password";
		}
	}

	@RequestMapping(value = "user/resendemail.htm", method = RequestMethod.POST)
	public String resendEmail(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String useremail = request.getParameter("username");
		Random rand = new Random();
		int randomNum1 = rand.nextInt(5000000);
		int randomNum2 = rand.nextInt(5000000);
		System.out.println("can we reach here?");
		try {
			String str = "http://localhost:8080/finalproject/user/validateemail.htm?email=" + useremail + "&key1=" + randomNum1
					+ "&key2=" + randomNum2;
			session.setAttribute("key1", randomNum1);
			session.setAttribute("key2", randomNum2);
			sendEmail(useremail,
					"Click on this link to activate your account : "+ str);
		} catch (Exception e) {
			System.out.println("Email cannot be sent");
		}
		
		return "user-created";
	}

	public void sendEmail(String useremail, String message) {
		System.out.println("Email will be sent");
		System.out.println(useremail);
		System.out.println(message);
		try {
			Email email = new SimpleEmail();
			email.setHostName("smtp.googlemail.com");
			email.setSmtpPort(465);
			email.setAuthenticator(new DefaultAuthenticator("contactapplication2018@gmail.com", "springmvc"));
			email.setSSLOnConnect(true);
			email.setFrom("ren.ziq@husky.neu.edu"); // This user email does not
													// exist
			email.setSubject("Password Reminder");
			email.setMsg(message); // Retrieve email from the DAO and send this
			email.addTo(useremail);
			email.send();
		} catch (EmailException e) {
			System.out.println("Email cannot be sent");
		}
	}

	@RequestMapping(value = "user/validateemail.htm", method = RequestMethod.GET)
	public String validateEmail(HttpServletRequest request, UserDAO userDao, ModelMap map) {

		// The user will be sent the following link when the use registers
		// This is the format of the email
		// http://hostname:8080/lab10/user/validateemail.htm?email=useremail&key1=<random_number>&key2=<body
		// of the email that when user registers>
		HttpSession session = request.getSession();
		String email = request.getParameter("email");
		int key1 = Integer.parseInt(request.getParameter("key1"));
		int key2 = Integer.parseInt(request.getParameter("key2"));
		System.out.println(session.getAttribute("key1") );
		System.out.println(session.getAttribute("key2") );
		
		
		if ((Integer)(session.getAttribute("key1")) == key1 && ((Integer)session.getAttribute("key2"))== key2) {
			try {
				System.out.println("HI________");
				boolean updateStatus = userDao.updateUser(email);
				if (updateStatus) {
					return "user-login";
				} else {

					return "error";
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			map.addAttribute("errorMessage", "Link expired , generate new link");
			map.addAttribute("resendLink", true);
			return "error";
		}

		return "user-login";

	}

}
