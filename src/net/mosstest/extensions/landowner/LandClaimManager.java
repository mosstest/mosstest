package net.mosstest.extensions.landowner;

public class LandClaimManager {
    public static ILandOwnershipProvider getProvider() {
        return ourInstance;
    }

    private static ILandOwnershipProvider ourInstance;

    public static void setProvider(ILandOwnershipProvider p){
        ourInstance = p;
    }

    private LandClaimManager() {
    }
}
