import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class Simplex extends JFrame {

    private JTextArea txtResultado;
    private JTextField txtFuncion;
    private JTextArea txtRestricciones;
    private JComboBox<String> comboTipos;

    public Simplex() {
        this.setSize(550, 700); 
        this.setTitle("Método Simplex");
        this.setResizable(false);
        this.initComponents();
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new FlowLayout());
    }

    private void initComponents() {
        JLabel lblFuncion = new JLabel("Función objetivo (ej: 3 + 5): ");
        txtFuncion = new JTextField();
        txtFuncion.setPreferredSize(new Dimension(450, 30));

        JLabel lblRestricciones = new JLabel("Restricciones (ej: 2 + 1 <= 8): ");
        txtRestricciones = new JTextArea(6,45);
        JScrollPane scrollRestricciones = new JScrollPane(txtRestricciones);

        JLabel lblTipo = new JLabel("Tipo: ");
        String[] tipos = {"Maximizar", "Minimizar"};
        comboTipos = new JComboBox<>(tipos);

        JButton btnEjecutar = new JButton("Calcular");
        btnEjecutar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ejecutarSimplex();
            }
        });

        JButton btnBorrar = new JButton("Borrar");
        btnBorrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                borrarCampos();
            }
        });

        JLabel lblResultado = new JLabel("Resultados: ");
        txtResultado = new JTextArea(20, 45);
        txtResultado.setEditable(false);
        JScrollPane scrollResultado = new JScrollPane(txtResultado);

        this.add(lblFuncion);
        this.add(txtFuncion);
        this.add(lblRestricciones);
        this.add(scrollRestricciones);
        this.add(lblTipo);
        this.add(comboTipos);
        this.add(btnEjecutar);
        this.add(btnBorrar);
        this.add(lblResultado);
        this.add(scrollResultado);
    }

    private void ejecutarSimplex() {
        String funcionObjetivo = txtFuncion.getText();
        String[] restricciones = txtRestricciones.getText().split("\n");
        String tipoOptimizacion = (String) comboTipos.getSelectedItem();

        if (funcionObjetivo.isEmpty() || restricciones.length == 0) {
            JOptionPane.showMessageDialog(null, "Por favor ingresa la función objetivo y al menos una restricción.");
            return;
        }

        try {
            double[] coefObjetivo = parseFuncionObjetivo(funcionObjetivo);
            double[][] matrizRestricciones = parseRestricciones(restricciones);

            simplexSolver(coefObjetivo, matrizRestricciones, tipoOptimizacion);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    private void borrarCampos() {
        txtFuncion.setText("");
        txtRestricciones.setText("");
        txtResultado.setText("");
        comboTipos.setSelectedIndex(0);
    }


    private double[] parseFuncionObjetivo(String funcion) {

        String[] coef = funcion.replace("-", "+-").split("\\+");
        double[] result = new double[coef.length];
        for (int i = 0; i < coef.length; i++) {
            try {
                result[i] = Double.parseDouble(coef[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Coeficiente inválido en la función objetivo: " + coef[i]);
            }
        }
        return result;
    }


    private double[][] parseRestricciones(String[] restricciones) {
        double[][] result = new double[restricciones.length][];
        for (int i = 0; i < restricciones.length; i++) {
            String restriccion = restricciones[i].trim();
            if (restriccion.isEmpty()) {
                continue;
            }

            restriccion = restriccion.replace("-", "+-");

            String[] partes = restriccion.split(">=|<=|>|<");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato de restricción inválido: " + restriccion);
            }

            String[] coef = partes[0].split("\\+");
            result[i] = new double[coef.length + 1];
            for (int j = 0; j < coef.length; j++) {
                try {
                    result[i][j] = Double.parseDouble(coef[j].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Coeficiente inválido en la restricción: " + restriccion);
                }
            }

            String constante = partes[1].trim();
            if (constante.isEmpty()) {
                throw new IllegalArgumentException("Término constante vacío en la restricción: " + restriccion);
            }
            try {
                result[i][coef.length] = Double.parseDouble(constante);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Término constante inválido en la restricción: " + restriccion);
            }

            if (restriccion.contains(">=")) {
                for (int j = 0; j < result[i].length; j++) {
                    result[i][j] *= -1;
                }
            }
        }
        return result;
    }

    private void simplexSolver(double[] coefObjetivo, double[][] matrizRestricciones, String tipoOptimizacion) {
        int numVariables = coefObjetivo.length;
        int numRestricciones = matrizRestricciones.length;
        int numColumnas = numVariables + numRestricciones + 1; 

        double[][] tableau = new double[numRestricciones + 1][numColumnas];

        for (int i = 0; i < numRestricciones; i++) {
            System.arraycopy(matrizRestricciones[i], 0, tableau[i], 0, numVariables);
            tableau[i][numVariables + i] = 1; 
            tableau[i][numColumnas - 1] = matrizRestricciones[i][numVariables]; 
        }

        for (int j = 0; j < numVariables; j++) {
            tableau[numRestricciones][j] = (tipoOptimizacion.equals("Minimizar") ? coefObjetivo[j] : -coefObjetivo[j]);
        }

        mostrarTabla("Tabla inicial", tableau);

        int iteracion = 1;
        while (!esOptimo(tableau)) {
            int columnaPivote = seleccionarColumnaPivote(tableau);
            int filaPivote = seleccionarFilaPivote(tableau, columnaPivote);
            realizarPivot(tableau, filaPivote, columnaPivote);
            mostrarTabla("Iteración " + iteracion++, tableau);
        }

        mostrarTabla("Solución óptima", tableau);
        mostrarResultadosOptimos(tableau, numVariables);
    }

    private void mostrarResultadosOptimos(double[][] tableau, int numVariables) {
        StringBuilder sb = new StringBuilder();
        sb.append("Valores óptimos:\n");

        double valorZ = tableau[tableau.length - 1][tableau[0].length - 1]; // Valor de Z

        for (int j = 0; j < numVariables; j++) {
            double valorOptimo = 0;
            for (int i = 0; i < tableau.length - 1; i++) {
                if (tableau[i][j] == 1) {
                    valorOptimo = tableau[i][tableau[0].length - 1]; 
                }
            }
            sb.append("Variable x").append(j + 1).append(" = ").append(String.format("%.2f", valorOptimo)).append("\n");
        }
        sb.append("Valor de Z = ").append(String.format("%.2f", valorZ)).append("\n");
        txtResultado.append(sb.toString() + "\n");
    }

    private boolean esOptimo(double[][] tableau) {
        for (double valor : tableau[tableau.length - 1]) {
            if (valor < 0) {
                return false;
            }
        }
        return true;
    }

    private int seleccionarColumnaPivote(double[][] tableau) {
        double minValor = 0;
        int columnaPivote = -1;
        for (int j = 0; j < tableau[0].length - 1; j++) {
            if (tableau[tableau.length - 1][j] < minValor) {
                minValor = tableau[tableau.length - 1][j];
                columnaPivote = j;
            }
        }
        return columnaPivote;
    }

    private int seleccionarFilaPivote(double[][] tableau, int columnaPivote) {
        double minCociente = Double.POSITIVE_INFINITY;
        int filaPivote = -1;

        for (int i = 0; i < tableau.length - 1; i++) {
            if (tableau[i][columnaPivote] > 0) {
                double cociente = tableau[i][tableau[0].length - 1] / tableau[i][columnaPivote];
                if (cociente < minCociente) {
                    minCociente = cociente;
                    filaPivote = i;
                }
            }
        }

        if (filaPivote == -1) {
            throw new IllegalArgumentException("El problema es ilimitado.");
        }
        return filaPivote;
    }

    private void realizarPivot(double[][] tableau, int filaPivote, int columnaPivote) {
        double pivote = tableau[filaPivote][columnaPivote];

        for (int j = 0; j < tableau[0].length; j++) {
            tableau[filaPivote][j] /= pivote; // Normalizar fila pivote
        }

        for (int i = 0; i < tableau.length; i++) {
            if (i != filaPivote) {
                double factor = tableau[i][columnaPivote];
                for (int j = 0; j < tableau[0].length; j++) {
                    tableau[i][j] -= factor * tableau[filaPivote][j];
                }
            }
        }
    }

    private void mostrarTabla(String titulo, double[][] tableau) {
        StringBuilder sb = new StringBuilder();
        sb.append(titulo).append("\n");
        for (double[] fila : tableau) {
            for (double valor : fila) {
                sb.append(String.format("%.2f", valor)).append("\t");
            }
            sb.append("\n");
        }
        txtResultado.append(sb.toString() + "\n\n");
    }

    public static void main(String[] args) {
        new Simplex();
    }
}
