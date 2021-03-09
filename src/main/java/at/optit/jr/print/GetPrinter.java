package at.optit.jr.print;

import static nginx.clojure.MiniConstants.CONTENT_TYPE;
import static nginx.clojure.MiniConstants.NGX_HTTP_OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.print.DocFlavor;
import javax.print.PrintService; 
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.Sides;

import nginx.clojure.java.ArrayMap;
import nginx.clojure.java.NginxJavaRingHandler;

public class GetPrinter implements NginxJavaRingHandler {

	public Object[] invoke(Map<String, Object> request) {
		List<String> printerCSV = new ArrayList<String>();
		printerCSV.add("printer,mediasizenames,trays,sides\r\n");
		getPrinter(printerCSV);

		return new Object[] { NGX_HTTP_OK, // http status 200
				ArrayMap.create(CONTENT_TYPE, "text/csv", "Content-Disposition","attachment; filename=printer.csv"), // headers map
				printerCSV.toArray()// response body can be string, File or Array/Collection of them
		};
	}

	private void getPrinter(List<String> printerCSV) {
		PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService printer : printServices) {
			StringBuilder mediaTrays = new StringBuilder();
			StringBuilder mediaSizes = new StringBuilder();
			StringBuilder duplexCapabilities = new StringBuilder();

			Media[] media = (Media[]) printer.getSupportedAttributeValues(Media.class,
					DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
			for (Media m : media) {
				if (m instanceof MediaSizeName) {
					addLOVValue(m.toString(), mediaSizes);
				}
				if (m instanceof MediaTray) {
					addLOVValue(m.toString(), mediaTrays);
				}
			}
			Object sidesObj = printer.getSupportedAttributeValues(Sides.class, DocFlavor.SERVICE_FORMATTED.PRINTABLE,
					null);
			if (sidesObj != null) {
				Sides[] sides = (Sides[]) sidesObj;
				for (Sides s : sides) {
					if (s instanceof Sides) {
						addLOVValue(s.toString(), duplexCapabilities);
					}
				}
			}
			printerCSV.add(printer.getName() + "," + mediaSizes + "," + mediaTrays + "," + duplexCapabilities + "\r\n");
		}
	}

	private static void addLOVValue(String value, StringBuilder LOVString) {
		if (LOVString.length() != 0) {
			LOVString.append(":");
		}
		LOVString.append(value);
	}
}