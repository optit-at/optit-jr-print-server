package at.optit.jr.print;

import static nginx.clojure.MiniConstants.CONTENT_TYPE;
import static nginx.clojure.MiniConstants.NGX_HTTP_OK;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.Sides;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import nginx.clojure.MiniConstants;
import nginx.clojure.java.ArrayMap;
import nginx.clojure.java.JavaLazyHeaderMap;
import nginx.clojure.java.NginxJavaRingHandler;

public class JRPrintHandler implements NginxJavaRingHandler {

	private Object test;
	private int calls = 0;

	public Object[] invoke(Map<String, Object> request) {
		calls++;

		String requestMethod = (String) request.get(MiniConstants.REQUEST_METHOD);
		String contentType = (String) request.get(MiniConstants.CONTENT_TYPE);

		JavaLazyHeaderMap headers = ((JavaLazyHeaderMap) request.get(MiniConstants.HEADERS));
		String sidesString = (String) headers.get(CustomHeaderNames.X_Sides);
		String mediaSizeNameString = (String) headers.get(CustomHeaderNames.X_MediaSizeName);
		// Selection of trays doesn't seem to work (java-bug?) 
//		String mediaTrayNameString = (String) headers.get(NTFCustomHeaderNames.X_MediaSizeName);

		Sides sides = null;
		MediaSizeName mediaSizeName = null;

		try {
			sides = (Sides) Sides.class.getDeclaredField(sidesString).get(null);
			mediaSizeName = (MediaSizeName) MediaSizeName.class.getDeclaredField(mediaSizeNameString).get(null);
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (sides == null) {
			sides = Sides.ONE_SIDED;
		}
		if (mediaSizeName == null) {
			mediaSizeName = MediaSizeName.ISO_A4;
		}

		String printerString = (String) headers.get(CustomHeaderNames.X_Printer);
		PrinterName printername = new PrinterName(printerString, null);

		String idString = (String) headers.get(CustomHeaderNames.X_ID);
		JobName id = new JobName("PRQ_ID: " + idString, null);

		// The jrprint file should be in the body of the request
		if (requestMethod.equals(MiniConstants.POST) && contentType.equals("application/octet-stream")) {

			FileInputStream fis = (FileInputStream) request.get(MiniConstants.BODY);
			SimpleExporterInput exporterInput = new SimpleExporterInput(fis);
			try {
				JRPrintHandler.print(sides, printername, mediaSizeName, id, exporterInput);
				return new Object[] { NGX_HTTP_OK, // http status 200
						ArrayMap.create(CONTENT_TYPE, "text/plain"), // headers map
						"Super ausgedruckt" + test + calls // response body can be string, File or Array/Collection of
															// them
				};
			} catch (JRException e) {
				e.printStackTrace();
				return new Object[] { MiniConstants.NGX_HTTP_INTERNAL_SERVER_ERROR, // http status 500
						ArrayMap.create(CONTENT_TYPE, "text/plain"), // headers map
						"Printing not successfull" // response body can be string, File or Array/Collection of them
				};
			}
		} else { // We don't want to reveal any secrets here just answer with status code 200
			return new Object[] { NGX_HTTP_OK, // http status 200
					ArrayMap.create(CONTENT_TYPE, "text/plain"), // headers map
					"Probably you sent the wrong content type!" // response body can be string, File or Array/Collection of them
			};
		}
	}

	public static void print(Sides sides, PrinterName printername, MediaSizeName mediaSizeName, JobName id,
			ExporterInput exporterInput) throws JRException {
		long start = System.currentTimeMillis();
		PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
		printRequestAttributeSet.add(mediaSizeName);
		printRequestAttributeSet.add(sides);
		printRequestAttributeSet.add(id);

		PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
		printServiceAttributeSet.add(printername);

		JRPrintServiceExporter exporter = new JRPrintServiceExporter();

		exporter.setExporterInput(exporterInput);

		SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
		configuration.setPrintRequestAttributeSet(printRequestAttributeSet);
		configuration.setPrintServiceAttributeSet(printServiceAttributeSet);
		configuration.setDisplayPageDialog(false);
		configuration.setDisplayPrintDialog(false);

		exporter.setConfiguration(configuration);

		exporter.exportReport();
		
		System.err.println("Start time    : " + (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z")).format(new Date()));
		System.err.println("ID            : " + id);
		System.err.println("Printing time : " + (System.currentTimeMillis() - start));
		System.err.println("---------------");
		
	}
}