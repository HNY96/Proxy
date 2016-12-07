package Proxy;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Font;

public class Login extends JFrame implements Runnable {

    private JPanel contentPane;
    private JTextField username;
    private JPasswordField password;

    /**
     * Launch the application.
     */
//    public static void main(String[] args) {
//        EventQueue.invokeLater(new Runnable() {
    public void run() {
        try {
            Login frame = new Login();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//        });
//    }

    /**
     * Create the frame.
     */
    public Login() {

        setTitle("Login                              ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        JButton btnNewButton = new JButton("Confirm");
        btnNewButton.setFont(new Font("Consolas", Font.PLAIN, 16));

        btnNewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String user = username.getText();

                char[] pass = password.getPassword();
                StringBuffer strBuf = new StringBuffer();
                for (int i = 0; i < pass.length; i++) {
                    strBuf.append(pass[i]);
                }
                if (user.equals("Tim") && strBuf.toString().equals("19550608")) {
                    Proxy.LoginFlag = true;
                    JOptionPane.showMessageDialog(contentPane, "Login successfully");
                } else {
                    Proxy.LoginFlag = false;
                    JOptionPane.showMessageDialog(contentPane, "Login failed");
                }

                dispose();
            }
        });
        btnNewButton.setBounds(78, 190, 104, 39);
        contentPane.add(btnNewButton);

        username = new JTextField();
        username.setBounds(126, 25, 271, 32);
        contentPane.add(username);
        username.setColumns(10);

        JLabel lblNewLabel = new JLabel("username");
        lblNewLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        lblNewLabel.setBounds(25, 33, 72, 24);
        contentPane.add(lblNewLabel);

        JLabel lblNewLabel_1 = new JLabel("password");
        lblNewLabel_1.setFont(new Font("Consolas", Font.PLAIN, 16));
        lblNewLabel_1.setBounds(25, 96, 72, 24);
        contentPane.add(lblNewLabel_1);

        password = new JPasswordField();
        password.setBounds(126, 88, 271, 32);
        contentPane.add(password);

        JButton btnNewButton_1 = new JButton("Cancel");
        btnNewButton_1.setFont(new Font("Consolas", Font.PLAIN, 16));
        btnNewButton_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        btnNewButton_1.setBounds(240, 190, 104, 39);
        contentPane.add(btnNewButton_1);
    }
}