package edu.cmu.emfta.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import edu.cmu.emfta.Event;
import edu.cmu.emfta.Gate;

public class Utils {
	public static void refreshWorkspace(IProgressMonitor monitor) {
		for (IProject ip : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			try {
				ip.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static IPath getPath(final URI resourceURI) {
		/*
		 * I don't really understand why this method does what it does, but
		 * the point seems to be to take a URI for a Resource that resembles
		 * "platform:/resource/xxx/yyy/zzz" and return the Eclipse IPath to
		 * the file for that Resource. This seems to involve removing the
		 * "/resource/" part.
		 * 
		 * --aarong
		 */

		// Is it a "plaform:" uri?
		if (resourceURI.scheme() != null && resourceURI.scheme().equalsIgnoreCase("platform")) {
			// Get the segments. See if the first is "resource"
			final String[] segments = resourceURI.segments();
			final StringBuffer path = new StringBuffer();

			if (segments.length >= 1) {
				final int firstSegment = segments[0].equals("resource") ? 1 : 0;
				final int lastIdx = segments.length - 1;
				for (int i = firstSegment; i < (lastIdx); i++) {
					path.append(segments[i]);
					path.append('/');
				}
				if (lastIdx >= 0) {
					path.append(segments[lastIdx]);
				}
			}
			return new Path(null, path.toString());
		} else if (resourceURI.isFile()) {
			return new Path(resourceURI.toFileString());
		} else {
			throw new IllegalArgumentException("Cannot decode URI protocol: " + resourceURI.scheme());
		}
	}

	public static void checkProbability(Event event) {
		Gate gate = event.getGate();
		IResource res = getResourceForModel(event);

		double result = 0;

		if (gate == null) {
			return;
		}
		switch (gate.getType()) {
		case AND: {
			result = 1;
			for (Event subEvent : gate.getEvents()) {
				result = result * getProbability(subEvent);
				checkProbability(subEvent);
			}
			break;
		}
		case OR: {
			result = 0;
			for (Event subEvent : gate.getEvents()) {
				result = result + getProbability(subEvent);
				checkProbability(subEvent);
			}
			break;
		}
		default: {
			System.out.println("[Utils] Unsupported for now");
			result = -1;
			break;
		}

		}

		if (result != event.getProbability()) {

			IMarker marker;

			System.out.println("[Utils] here");

			String msg = "Probability mismatch for event " + event.getName() + "declared=" + event.getProbability()
					+ ";actual=" + result;
			try {
				marker = res.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.MESSAGE, msg);
				marker.setAttribute(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
				marker.setAttribute(Activator.EMFTA_MARKER, "true");
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}

			System.out.println("[Utils] " + msg);
		}
	}

	public static double getProbability(Event event) {
		Gate gate = event.getGate();
		double result;

		if (gate != null) {
			switch (gate.getType()) {
			case AND: {
				result = 1;
				for (Event subEvent : gate.getEvents()) {
					result = result * getProbability(subEvent);
				}
				break;
			}
			case OR: {
				result = 0;
				for (Event subEvent : gate.getEvents()) {
					result = result + getProbability(subEvent);
				}
				break;
			}
			default: {
				System.out.println("[Utils] Unsupported for now");
				result = -1;
				break;
			}
			}
			System.out.println("[Utils] Probability for " + event.getName() + ":" + result);

		} else {
			result = event.getProbability();
		}
		return result;
	}

	/**
	 * Get the IResource object associated with the EObject
	 * @param obj
	 * @return - the IResource associated with this EObject
	 */
	public static IResource getResourceForModel(EObject obj) {
		URI uri = obj.eResource().getURI();
		// assuming platform://resource/project/path/to/file
		String project = uri.segment(1);
		IPath path = new Path(uri.path()).removeFirstSegments(2);
		return ResourcesPlugin.getWorkspace().getRoot().getProject(project).findMember(path);
	}

	/**
	 * Remove all Markers for the eobject passed as parameter
	 * @param obj - the object associated with all the markers
	 */
	public static void removeAllMarkers(EObject obj) {
		IResource res = getResourceForModel(obj);
		try {
			res.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			System.out.println("[Utils] Try to delete marker for object " + obj);

			e.printStackTrace();
		}
	}
}
