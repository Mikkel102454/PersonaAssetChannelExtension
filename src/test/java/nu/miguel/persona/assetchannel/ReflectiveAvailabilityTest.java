package nu.miguel.persona.assetchannel;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReflectiveAvailabilityTest {
    @Test void absentPluginIsUnavailableWithClearError() {
        PluginManager manager = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            ReflectiveAssetChannelAdapter adapter = new ReflectiveAssetChannelAdapter();
            assertFalse(adapter.available());
            BridgeException error = assertThrows(BridgeException.class, () -> adapter.stopAllSounds(null));
            assertTrue(error.getMessage().contains("not installed or enabled"));
        }
    }

    @Test void disabledPluginIsUnavailable() {
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        when(manager.getPlugin("AssetChannel")).thenReturn(plugin);
        when(plugin.isEnabled()).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertFalse(new ReflectiveAssetChannelAdapter().available());
        }
    }

    @Test void incompatiblePluginClassloaderIsUnavailable() {
        PluginManager manager = mock(PluginManager.class);
        Plugin plugin = mock(Plugin.class);
        when(manager.getPlugin("AssetChannel")).thenReturn(plugin);
        when(plugin.isEnabled()).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            assertFalse(new ReflectiveAssetChannelAdapter().available());
        }
    }
}
